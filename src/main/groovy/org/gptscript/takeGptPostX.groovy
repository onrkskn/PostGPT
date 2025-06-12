package org.gptscript

import org.openqa.selenium.support.ui.ExpectedConditions
import org.openqa.selenium.support.ui.WebDriverWait
import java.time.Duration
import org.openqa.selenium.WebDriver
import org.openqa.selenium.chrome.ChromeDriver
import org.openqa.selenium.chrome.ChromeOptions
import org.openqa.selenium.By
import org.openqa.selenium.WebElement
import java.util.Properties
import org.yaml.snakeyaml.Yaml
import groovy.json.JsonSlurper
import groovy.json.JsonOutput
import java.time.LocalDate

System.setProperty("webdriver.chrome.driver", "/usr/local/bin/chromedriver")

Properties props = new Properties()
File propsFile = new File("/Users/skyworks/Desktop/GROOVY/PostGpt/config.properties")
props.load(propsFile.newInputStream())
String apiKey = props.getProperty("openai.api.key")

String generateText(String prompt, String apiKey) {
    def url = new URL("https://api.openai.com/v1/chat/completions")
    HttpURLConnection connection = (HttpURLConnection) url.openConnection()
    connection.setRequestMethod("POST")
    connection.setRequestProperty("Content-Type", "application/json")
    connection.setRequestProperty("Authorization", "Bearer " + apiKey)
    connection.doOutput = true

    def requestPayload = [
            model: "gpt-4o-mini",
            messages: [[role: "user", content: prompt]],
            max_tokens: 100
    ]
    def jsonPayload = JsonOutput.toJson(requestPayload)
    connection.outputStream.withWriter("UTF-8") { writer ->
        writer.write(jsonPayload)
    }
    int responseCode = connection.getResponseCode()
    println("Response Code: " + responseCode)

    def response
    if (responseCode == HttpURLConnection.HTTP_OK) {
        response = connection.inputStream.withReader("UTF-8") { reader ->
            new JsonSlurper().parse(reader)
        }
    } else {
        response = connection.errorStream.withReader("UTF-8") { reader ->
            new JsonSlurper().parse(reader)
        }
        println("Hata Mesajı: " + response)
        throw new IOException("API request failed with response code " + responseCode + " and error message: " + response)
    }
    return response.choices[0].message.content.trim()
}

Yaml msgYaml = new Yaml()
File promptFile = new File("/Users/skyworks/Desktop/GROOVY/PostGpt/promptMsg.yaml")
Map promptsData = msgYaml.load(promptFile.newInputStream())
List<String> dailyPrompts = promptsData.get("prompts")

def todayIndex = LocalDate.now().getDayOfMonth() % dailyPrompts.size()
String prompt = dailyPrompts[todayIndex-1]
String tweetContent = generateText(prompt,apiKey)

//Yaml parameter seklinde okuma
Yaml yaml = new Yaml()
File yamlFile = new File("/Users/skyworks/Desktop/GROOVY/PostGpt/credentials.yaml")
Map data = yaml.load(yamlFile.newInputStream())
List<Map> accounts = data.get("accounts")

accounts.each { account ->

    String username = account.get("username")
    String password = account.get("password")

//ChromeOptions options = new ChromeOptions()
//options.addArguments("user-data-dir=/Users/skyworks/Library/Application Support/Google/Chrome")
//options.addArguments("profile-directory=Profile 10")

    WebDriver driver = new ChromeDriver()

    try {
        driver.get("https://x.com/login")
        Thread.sleep(2000)

        String title = driver.getTitle()
        println("Sayfa başlığı: ${title}")


        if (title.contains("Anasayfa / X")) {
            println("Zaten giriş yapılmış, post atılacak...")
            postMessage(driver, tweetContent)
        } else {
            println("X hesabına erişim sağlanamadı, farklı bir sayfa açıldı.")
            Thread.sleep(2000)
            driver.findElement(By.cssSelector("input[type='text']")).sendKeys(username)
            driver.findElement(By.xpath("//span[text()='İleri' or text()='Next']")).click()
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20))
            wait.until(ExpectedConditions.visibilityOfElementLocated(By.cssSelector("input[type='password']")))

            driver.findElement(By.cssSelector("input[type='password']")).sendKeys(password)

            WebElement loginButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[@data-testid='LoginForm_Login_Button']")))
            loginButton.click()
            Thread.sleep(3000)
            postMessage(driver, tweetContent)
            Thread.sleep(5000)
        }

    } catch (Exception e) {
        println("Bir hata oluştu: ${e.message}")
    } finally {
        driver.quit()
    }
}

def postMessage(WebDriver driver, String messageContent) {
    try {
        WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20))
        WebElement messageArea = wait.until(ExpectedConditions.visibilityOfElementLocated(By.xpath("//div[@data-testid='tweetTextarea_0']")))
        Thread.sleep(3000)
        messageArea.click()
        messageArea.sendKeys(messageContent)

        WebElement sendButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//button[@data-testid='tweetButtonInline']")))
        sendButton.click()
        Thread.sleep(3000)
        println("Mesaj gönderildi: ${messageContent}")
    } catch (Exception e) {
        println("Mesaj gönderilirken bir hata oluştu: ${e.message}")
    }
}
