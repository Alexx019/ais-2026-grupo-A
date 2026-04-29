package es.codeurjc.service;

import es.codeurjc.model.Account;
import es.codeurjc.model.User;
import es.codeurjc.model.Notification;
import es.codeurjc.model.Transaction;
import es.codeurjc.repository.AccountRepository;
import es.codeurjc.repository.TransactionRepository;
import es.codeurjc.service.notifications.EmailNotificationService;
import es.codeurjc.service.notifications.SmsNotificationService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Service
public class AccountService {

    private final AccountRepository accountRepository;
    private final TransactionRepository transactionRepository;
    private final EmailNotificationService emailService;
    private final SmsNotificationService smsService;
    private final RandomService randomService;

    public AccountService(AccountRepository accountRepository,
                          TransactionRepository transactionRepository,
                          EmailNotificationService emailService,
                          SmsNotificationService smsService,
                          RandomService randomService) {
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
        this.emailService = emailService;
        this.smsService = smsService;
        this.randomService = randomService;
    }


    public Account createAccount(User user, Account.AccountType accountType) {
        String accountNumber = generateAccountNumber();
        Account account = new Account(accountNumber, accountType, 0);
        account.setUser(user);
        return accountRepository.save(account);
    }


    private String generateAccountNumber() {
        return String.format("ES%010d", randomService.nextInt(1000000000));
    }


    public Account getAccount(String accountNumber) {
        return accountRepository.findByAccountNumber(accountNumber)
                .orElseThrow(() -> new IllegalArgumentException("Account not found"));
    }


    public List<Account> getUserAccounts(User user) {
        return accountRepository.findByUser(user);
    }

    @Transactional
    public Account deposit(String accountNumber, double amount, String description) {
        if (amount == 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (amount < 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (amount > 10000) {
            throw new IllegalArgumentException("Amount exceeds maximum deposit limit");
        }
        if (amount > 50000) {
            throw new IllegalArgumentException("Amount exceeds maximum deposit limit");
        }

        Account account = getAccount(accountNumber);


        if (account.getUser().isBanned()) {
            throw new IllegalArgumentException("User account is banned");
        }
        account.deposit(amount);

        Transaction transaction = new Transaction(account, Transaction.TransactionType.DEPOSIT,
                amount, description);
        transactionRepository.save(transaction);

        Account savedAccount = accountRepository.save(account);

        sendDepositNotification(account,amount);

        return savedAccount;
    }


    @Transactional
    public Account deposit(String accountNumber, double amount) {
        return this.deposit(accountNumber, amount, "Quick deposit");
    }

    @Transactional
    public Account withdraw(String accountNumber, double amount, String description) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }

        if (amount > 5000) {
            throw new IllegalArgumentException("Amount exceeds maximum withdrawal limit");
        }

        Account account = getAccount(accountNumber);

        if (account.getUser().isBanned()) {
            throw new IllegalArgumentException("User account is banned");
        }


        if (account.getBalance() < amount) {
            throw new IllegalArgumentException("Insufficient funds");
        }

        account.withdraw(amount);

        Transaction transaction = new Transaction(account, Transaction.TransactionType.WITHDRAWAL,
                amount, description);
        transactionRepository.save(transaction);

        Account savedAccount = accountRepository.save(account);

        User.NotificationType notifType = account.getUser().getNotificationType();
        if (notifType == User.NotificationType.EMAIL) {
            emailService.sendNotification(
                    account.getUser(),
                    Notification.NotificationType.WITHDRAWAL,
                    "Withdrawal Confirmation",
                    String.format("Withdrawal of %.2f EUR. New balance: %.2f EUR", amount, account.getBalance()));
        } else if (notifType == User.NotificationType.SMS) {
            smsService.sendNotification(
                    account.getUser(),
                    Notification.NotificationType.WITHDRAWAL,
                    "Withdrawal",
                    String.format("Withdrawal of %.2f EUR. New balance: %.2f EUR", amount, account.getBalance()));
        }

        return savedAccount;
    }


    @Transactional
    public void transfer(String fromAccountNumber, String toAccountNumber, double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (amount > 20000) {
            throw new IllegalArgumentException("Amount exceeds maximum transfer limit");
        }

        Account sourceAccount = getAccount(fromAccountNumber);
        Account destinationAccount = getAccount(toAccountNumber);

        if (sourceAccount.getUser().isBanned()) {
            throw new IllegalArgumentException("Source user account is banned");
        }

        if (destinationAccount.getUser().isBanned()) {
            throw new IllegalArgumentException("Destination user account is banned");
        }

        if (sourceAccount.getAccountNumber().equals(destinationAccount.getAccountNumber())) { //Comparación usando la funcion equals()
            throw new IllegalArgumentException("Cannot transfer to same account");
        }

        if (sourceAccount.getBalance() < amount) {
            throw new IllegalArgumentException("Insufficient funds");
        }

        sourceAccount.withdraw(amount);
        destinationAccount.deposit(amount);

        Transaction sentTransaction = new Transaction(sourceAccount,
                Transaction.TransactionType.TRANSFER_SENT,
                amount,
                "Transfer to " + toAccountNumber);
        sentTransaction.setDestinationAccountNumber(toAccountNumber);
        transactionRepository.save(sentTransaction);

        Transaction receivedTransaction = new Transaction(destinationAccount,
                Transaction.TransactionType.TRANSFER_RECEIVED,
                amount,
                "Transfer from " + fromAccountNumber);
        receivedTransaction.setDestinationAccountNumber(fromAccountNumber);
        transactionRepository.save(receivedTransaction);

        accountRepository.save(sourceAccount);
        accountRepository.save(destinationAccount);

        User.NotificationType notifType = sourceAccount.getUser().getNotificationType();
        if (notifType == User.NotificationType.EMAIL) {
            emailService.sendNotification(
                    sourceAccount.getUser(),
                    Notification.NotificationType.TRANSFER,
                    "Transfer Sent",
                    String.format("Transfer of %.2f EUR to %s. New balance: %.2f EUR", amount, toAccountNumber, sourceAccount.getBalance()));
        } else if (notifType == User.NotificationType.SMS) {
            smsService.sendNotification(
                    sourceAccount.getUser(),
                    Notification.NotificationType.TRANSFER,
                    "Transfer Sent",
                    String.format("Transfer of %.2f EUR to %s. New balance: %.2f EUR", amount, toAccountNumber, sourceAccount.getBalance()));
        }

        User.NotificationType notifTypeTo = destinationAccount.getUser().getNotificationType();
        if (notifTypeTo == User.NotificationType.EMAIL) {
            emailService.sendNotification(
                    destinationAccount.getUser(),
                    Notification.NotificationType.TRANSFER,
                    "Transfer Received",
                    String.format("Transfer of %.2f EUR from %s. New balance: %.2f EUR",
                            amount, fromAccountNumber, destinationAccount.getBalance()));
        } else if (notifTypeTo == User.NotificationType.SMS) {
            smsService.sendNotification(
                    destinationAccount.getUser(),
                    Notification.NotificationType.TRANSFER,
                    "Transfer Received",
                    String.format("Transfer of %.2f EUR from %s. New balance: %.2f EUR", amount, fromAccountNumber, destinationAccount.getBalance()));
        }
    }


    public void deleteAccount(String accountNumber) {
        Account account = getAccount(accountNumber);

        if (account.getBalance() != 0) {
            throw new IllegalArgumentException("Cannot delete account with non-zero balance");
        }

        accountRepository.delete(account);
    }


    public double getBalance(String accountNumber) {
        Account account = getAccount(accountNumber);
        return account.getBalance();
    }


    public List<Transaction> getTransactions(String accountNumber) {
        Account account = getAccount(accountNumber);
        return transactionRepository.findByAccountOrderByTimestampDesc(account);
    }
    private void sendDepositNotification(Account account, double amount) {
        User user = account.getUser();
        User.NotificationType notifType = user.getNotificationType();

        String subject = "Deposit Confirmation";
        double balance = account.getBalance();

        if (notifType == User.NotificationType.EMAIL) {
            emailService.sendNotification(
                    user,
                    Notification.NotificationType.DEPOSIT,
                    subject,
                    String.format("Deposit of %.2f EUR. New balance: %.2f EUR", amount, balance));
        } else if (notifType == User.NotificationType.SMS) {
            smsService.sendNotification(
                    user,
                    Notification.NotificationType.DEPOSIT,
                    subject,
                    String.format("Deposit: %.2f EUR. Balance: %.2f EUR", amount, balance));
        }
    }


}