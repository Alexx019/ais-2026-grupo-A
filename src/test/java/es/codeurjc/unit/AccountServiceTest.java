package es.codeurjc.unit;

import es.codeurjc.model.Account;
import es.codeurjc.model.Notification;
import es.codeurjc.model.Transaction;
import es.codeurjc.model.User;
import es.codeurjc.repository.AccountRepository;
import es.codeurjc.repository.TransactionRepository;
import es.codeurjc.service.AccountService;
import es.codeurjc.service.RandomService;
import es.codeurjc.service.notifications.EmailNotificationService;
import es.codeurjc.service.notifications.SmsNotificationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.*;

public class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private TransactionRepository transactionRepository;

    @Mock
    private EmailNotificationService emailService;

    @Mock
    private SmsNotificationService smsService;

    @Mock
    private RandomService randomService;

    @InjectMocks
    private AccountService accountService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    // Helpers

    private User buildEmailUser() {
        User user = new User();
        user.setNotificationType(User.NotificationType.EMAIL);
        user.setEmail("user@test.com");
        user.setPhone("600000000");
        return user;
    }

    private User buildSmsUser() {
        User user = new User();
        user.setNotificationType(User.NotificationType.SMS);
        user.setEmail("user@test.com");
        user.setPhone("600000000");
        return user;
    }

    private Account buildAccount(String accountNumber, Account.AccountType type, double balance, User user) {
        Account account = new Account(accountNumber, type, balance);
        account.setUser(user);
        return account;
    }


    @Test
    void testCreateAccount_ReturnsNewAccount() {
        User user = buildEmailUser();

        when(randomService.nextInt(1000000000)).thenReturn(123456789);
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account result = accountService.createAccount(user, Account.AccountType.SAVINGS);

        assertNotNull(result);
        assertEquals("ES0123456789", result.getAccountNumber());
        assertEquals(Account.AccountType.SAVINGS, result.getAccountType());
        assertEquals(0, result.getBalance(), 0.001);
        assertEquals(user, result.getUser());
        verify(accountRepository).save(any(Account.class));
    }

    @Test
    void testGetAccount_WhenExists_ReturnsAccount() {
        String accountNumber = "ES1234567890";
        Account account = buildAccount(accountNumber, Account.AccountType.SAVINGS, 100, buildEmailUser());

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));

        Account result = accountService.getAccount(accountNumber);

        assertEquals(account, result);
    }

    @Test
    void testGetAccount_WhenNotExists_ThrowsException() {
        String accountNumber = "ES0000000000";

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> accountService.getAccount(accountNumber)
        );

        assertEquals("Account not found", ex.getMessage());
    }

    @Test
    void testGetUserAccounts_ReturnsList() {
        User user = buildEmailUser();
        List<Account> accounts = List.of(
                buildAccount("ES1111111111", Account.AccountType.CHECKING, 100, user),
                buildAccount("ES2222222222", Account.AccountType.SAVINGS, 200, user)
        );

        when(accountRepository.findByUser(user)).thenReturn(accounts);

        List<Account> result = accountService.getUserAccounts(user);

        assertEquals(accounts, result);
    }


    @Test
    void testDeposit_WithValidAmount_UpdatesBalanceAndSendsEmail() {
        String accountNumber = "ES1234567890";
        User user = buildEmailUser();
        Account account = buildAccount(accountNumber, Account.AccountType.SAVINGS, 100, user);

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account result = accountService.deposit(accountNumber, 50, "Salary");

        assertEquals(150, result.getBalance(), 0.001);
        verify(transactionRepository).save(any(Transaction.class));
        verify(accountRepository).save(account);
        verify(emailService).sendNotification(
                eq(user),
                eq(Notification.NotificationType.DEPOSIT),
                eq("Deposit Confirmation"),
                argThat(s -> s.contains("150,00") || s.contains("150.00"))
        );
        verifyNoInteractions(smsService);
    }

    @Test
    void testDeposit_WithValidAmount_UpdatesBalanceAndSendsSms() {
        String accountNumber = "ES1234567891";
        User user = buildSmsUser();
        Account account = buildAccount(accountNumber, Account.AccountType.SAVINGS, 100, user);

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account result = accountService.deposit(accountNumber, 25, "Cash in");

        assertEquals(125, result.getBalance(), 0.001);
        verify(transactionRepository).save(any(Transaction.class));
        verify(accountRepository).save(account);

        verify(smsService).sendNotification(
                eq(user),
                eq(Notification.NotificationType.DEPOSIT),
                eq("Deposit Confirmation"),
                matches(".Balance: 125[.,]00 EUR.")
        );
        verifyNoInteractions(emailService);
    }

    @Test
    void testDeposit_WithNegativeAmount_ThrowsException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> accountService.deposit("ES123", -1, "Invalid")
        );

        assertEquals("Amount must be positive", ex.getMessage());
        verifyNoInteractions(accountRepository, transactionRepository, emailService, smsService);
    }

    @Test
    void testDeposit_WithZeroAmount_ThrowsException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> accountService.deposit("ES123", 0, "Invalid")
        );

        assertEquals("Amount must be positive", ex.getMessage());
        verifyNoInteractions(accountRepository, transactionRepository, emailService, smsService);
    }

    @Test
    void testDeposit_ExceedsLimit_ThrowsException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> accountService.deposit("ES123", 10001, "Too much")
        );

        assertEquals("Amount exceeds maximum deposit limit", ex.getMessage());
        verifyNoInteractions(accountRepository, transactionRepository, emailService, smsService);
    }

    @Test
    void testQuickDeposit_WorksCorrectly() {
        String accountNumber = "ES1234567892";
        User user = buildEmailUser();
        Account account = buildAccount(accountNumber, Account.AccountType.SAVINGS, 100, user);

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account result = accountService.deposit(accountNumber, 40);

        assertEquals(140, result.getBalance(), 0.001);

        ArgumentCaptor<Transaction> transactionCaptor = ArgumentCaptor.forClass(Transaction.class);
        verify(transactionRepository).save(transactionCaptor.capture());
        assertEquals(Transaction.TransactionType.DEPOSIT, transactionCaptor.getValue().getType());
        assertEquals("Quick deposit", transactionCaptor.getValue().getDescription());

        verify(emailService).sendNotification(
                eq(user),
                eq(Notification.NotificationType.DEPOSIT),
                eq("Deposit Confirmation"),
                matches(".New balance: 140.00 EUR.".replace(".", "[.,]"))
        );
    }

    @Test
    void testWithdraw_WithValidAmount_UpdatesBalanceAndSendsEmail() {
        String accountNumber = "ES2234567890";
        User user = buildEmailUser();
        Account account = buildAccount(accountNumber, Account.AccountType.CHECKING, 100, user);

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account result = accountService.withdraw(accountNumber, 40, "ATM");

        assertEquals(60, result.getBalance(), 0.001);
        verify(transactionRepository).save(any(Transaction.class));
        verify(accountRepository).save(account);
        verify(emailService).sendNotification(
                eq(user),
                eq(Notification.NotificationType.WITHDRAWAL),
                eq("Withdrawal Confirmation"),
                argThat(s -> s.contains("60,00") || s.contains("60.00"))
        );
        verifyNoInteractions(smsService);
    }

    @Test
    void testWithdraw_WithValidAmount_UpdatesBalanceAndSendsSms() {
        String accountNumber = "ES2234567891";
        User user = buildSmsUser();
        Account account = buildAccount(accountNumber, Account.AccountType.CHECKING, 200, user);

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account result = accountService.withdraw(accountNumber, 50, "ATM");

        assertEquals(150, result.getBalance(), 0.001);
        verify(transactionRepository).save(any(Transaction.class));
        verify(accountRepository).save(account);
        verify(smsService).sendNotification(
                eq(user),
                eq(Notification.NotificationType.WITHDRAWAL),
                eq("Withdrawal"),
                argThat(s -> s.contains("150,00") || s.contains("150.00")) // Solución aquí
        );
    }

    @Test
    void testWithdraw_InsufficientFunds_ThrowsException() {
        String accountNumber = "ES2234567892";
        Account account = buildAccount(accountNumber, Account.AccountType.CHECKING, 20, buildEmailUser());

        when(accountRepository.findByAccountNumber(accountNumber)).thenReturn(Optional.of(account));

        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> accountService.withdraw(accountNumber, 40, "ATM")
        );

        assertEquals("Insufficient funds", ex.getMessage());
        verify(transactionRepository, never()).save(any());
        verify(accountRepository, never()).save(any());
        verifyNoInteractions(emailService, smsService);
    }

    @Test
    void testWithdraw_NegativeAmount_ThrowsException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> accountService.withdraw("ES123", -1, "ATM")
        );

        assertEquals("Amount must be positive", ex.getMessage());
        verifyNoInteractions(accountRepository, transactionRepository, emailService, smsService);
    }

    @Test
    void testWithdraw_ExceedsLimit_ThrowsException() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> accountService.withdraw("ES123", 5001, "ATM")
        );

        assertEquals("Amount exceeds maximum withdrawal limit", ex.getMessage());
        verifyNoInteractions(accountRepository, transactionRepository, emailService, smsService);
    }

}
