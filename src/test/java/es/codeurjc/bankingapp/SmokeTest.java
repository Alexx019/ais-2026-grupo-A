package es.codeurjc.bankingapp; // Asegúrate de que el package coincida con tu proyecto

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class SmokeTest {

    private WebDriver driver;
    private String appUrl = System.getenv("APP_URL");
    private String expectedVersion = System.getenv("EXPECTED_VERSION");

    @BeforeEach
    public void setup() {
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless"); // Necesario para ejecutar en GitHub Actions
        driver = new ChromeDriver(options);
    }

    @Test
    public void verifyDeploymentAndVersion() {
        // 1. Navegar a la aplicación en Azure
        driver.get(appUrl);

        // 2. Buscar el elemento que contiene la versión (según el commit de la Tarea 1)
        // Nota: Ajusta el selector ID o clase según cómo implementaras la Tarea 1, paso 27
        WebElement versionElement = driver.findElement(By.id("version"));
        String currentVersion = versionElement.getText();

        // 3. Verificar que la versión coincide
        assertTrue(currentVersion.contains(expectedVersion),
                "La versión en la web (" + currentVersion + ") no coincide con la esperada (" + expectedVersion + ")");
    }

    @AfterEach
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }
}