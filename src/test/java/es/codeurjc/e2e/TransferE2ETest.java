package es.codeurjc.e2e;

import org.junit.jupiter.api.*;
import org.openqa.selenium.*;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.crypto.password.PasswordEncoder;

import es.codeurjc.model.Account;
import es.codeurjc.model.User;
import es.codeurjc.repository.AccountRepository;
import es.codeurjc.repository.NotificationRepository;
import es.codeurjc.repository.UserRepository;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class TransferE2ETest {

    @LocalServerPort
    private int port;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private WebDriver driver;
    private WebDriverWait wait;
    private String baseUrl;

    private User userA;
    private User userB;
    private Account accountA1;
    private Account accountA2;
    private Account accountB1;

    @BeforeAll
    void setUpDriver() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless=new", "--no-sandbox", "--disable-dev-shm-usage");
        driver = new ChromeDriver(options);
        wait = new WebDriverWait(driver, Duration.ofSeconds(10));
        baseUrl = "http://localhost:" + port;
    }

    @AfterAll
    void tearDownDriver() {
        if (driver != null) {
            driver.quit();
        }
    }


    @BeforeEach
    void setUpData() {
        long ts = System.currentTimeMillis();

        userA = userRepository.save(buildUser("usera_" + ts, "11111111A",
                "usera_" + ts + "@test.com", "+34600000001"));
        accountA1 = buildAccount("ESTA1" + ts, Account.AccountType.CHECKING, 5000.0, userA);
        accountA2 = buildAccount("ESTA2" + ts, Account.AccountType.SAVINGS,  2000.0, userA);

        userB = userRepository.save(buildUser("userb_" + ts, "22222222B",
                "userb_" + ts + "@test.com", "+34600000002"));
        accountB1 = buildAccount("ESTB1" + ts, Account.AccountType.CHECKING, 1000.0, userB);
    }

    @AfterEach
    void cleanUpData() {
        driver.manage().deleteAllCookies();

        notificationRepository.deleteAll(notificationRepository.findByUser(userA));
        notificationRepository.deleteAll(notificationRepository.findByUser(userB));

        accountRepository.deleteAll(accountRepository.findByUser(userA));
        accountRepository.deleteAll(accountRepository.findByUser(userB));
        userRepository.delete(userA);
        userRepository.delete(userB);
    }


    private User buildUser(String username, String dni, String email, String phone) {
        User u = new User();
        u.setUsername(username);
        u.setPassword(passwordEncoder.encode("password123"));
        u.setFirstName("Test");
        u.setLastName("User");
        u.setDni(dni);
        u.setEmail(email);
        u.setPhone(phone);
        u.setRegistrationDate(LocalDate.now());
        u.setMonthlyIncome(3000.0);
        u.setRoles(List.of("CUSTOMER"));
        return u;
    }

    private Account buildAccount(String number, Account.AccountType type, double balance, User owner) {
        Account a = new Account(number, type, balance);
        a.setUser(owner);
        return accountRepository.save(a);
    }

    private void login(String username) {
        driver.get(baseUrl + "/login");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.name("username")));
        driver.findElement(By.name("username")).sendKeys(username);
        driver.findElement(By.name("password")).sendKeys("password123");
        driver.findElement(By.cssSelector("button[type='submit']")).click();
        wait.until(ExpectedConditions.urlContains("/dashboard"));
    }

    private void fillAndSubmitTransfer(String fromAccountNumber, String toAccountNumber, String amount) {
        driver.get(baseUrl + "/transfer");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("fromAccount")));

        new Select(driver.findElement(By.id("fromAccount")))
                .selectByValue(fromAccountNumber);

        WebElement toInput = driver.findElement(By.id("toAccount"));
        toInput.clear();
        toInput.sendKeys(toAccountNumber);

        WebElement amountInput = driver.findElement(By.id("amount"));
        amountInput.clear();
        amountInput.sendKeys(amount);

        driver.findElement(By.id("transferSubmit")).click();
    }

    private double readBalanceFromDashboard(String accountNumber) {
        driver.get(baseUrl + "/dashboard");
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("balance-" + accountNumber)));
        String text = driver.findElement(By.id("balance-" + accountNumber)).getText();
        return Double.parseDouble(text.replace(",", ".").trim());
    }

    private String waitForSuccessMessage() {
        wait.until(ExpectedConditions.visibilityOfElementLocated(By.id("successMessage")));
        return driver.findElement(By.id("successMessage")).getText();
    }


    @Test
    void transferBetweenOwnAccounts() {
        login(userA.getUsername());

        double amount = 500.0;
        fillAndSubmitTransfer(accountA1.getAccountNumber(), accountA2.getAccountNumber(), "500");

        String msg = waitForSuccessMessage();
        assertTrue(msg.contains("Transfer completed successfully"),
                "Unexpected success message: " + msg);

        assertEquals(5000.0 - amount, readBalanceFromDashboard(accountA1.getAccountNumber()), 0.01);
        assertEquals(2000.0 + amount, readBalanceFromDashboard(accountA2.getAccountNumber()), 0.01);
    }
}