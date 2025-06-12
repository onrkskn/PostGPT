#!/usr/bin/env groovy
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


System.setProperty("webdriver.chrome.driver", "/usr/local/bin/chromedriver")

//Tek prop okunacagi zaman
//Properties props = new Properties();
//File propsFile = new File("/Users/skyworks/Desktop/GROOVY/TestScript/credentials.properties")
//props.load(propsFile.newInputStream())

Yaml yaml = new Yaml()
File yamlFile = new File("/Users/skyworks/Desktop/GROOVY/TestScript/credentials.yaml")
Map data = yaml.load(yamlFile.newInputStream())
List<Map> accounts = data.get("accounts")

accounts.each { account ->

String username = account.get("username")
String password = account.get("password")

//ChromeOptions options = new ChromeOptions()
//options.addArguments("user-data-dir=/Users/skyworks/Library/Application Support/Google/Chrome")
//options.addArguments("profile-directory=Profile 10")

// WebDriver'ı başlat
WebDriver driver = new ChromeDriver()

try {
  driver.get("https://x.com/login")
  Thread.sleep(2000)

  String title = driver.getTitle()
  println("Sayfa başlığı: ${title}")


  if (title.contains("Anasayfa / X")) {
    println("Zaten giriş yapılmış, post atılacak...")
    postMessage(driver)
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
    String postUrl = "https://x.com/Elminselimov5/status/1852992497535811957"
    driver.get(postUrl)
    Thread.sleep(2000)

    // Gönderiyi beğen
    likePost(driver)

    // Gönderiyi repost et
    repostPost(driver)
  }


  } catch (Exception e) {
    println("Bir hata oluştu: ${e.message}")
  } finally {
    driver.quit()
  }
}

def likePost(WebDriver driver) {
  WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20))
  try {
    // begeni button
    WebElement likeButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(
            "(//button[@data-testid='like' or contains(@aria-label, 'Beğen') or contains(@aria-label, 'Like')])[1]"
    )))
    likeButton.click()
    Thread.sleep(3000)
    println("Ana gönderi beğenildi!")
  } catch (Exception e) {
    println("Beğenme işlemi sırasında bir hata oluştu: ${e.message}")
  }
}

def repostPost(WebDriver driver) {
  WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(20))
  try {
    // repost button bul
    WebElement retweetButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath(
            "(//button[@data-testid='retweet' or contains(@aria-label, 'Yeniden gönderi') or contains(@aria-label, 'Retweet')])[1]"
    )))
    retweetButton.click()
    Thread.sleep(3000)
    //onayla
    WebElement confirmRetweetButton = wait.until(ExpectedConditions.elementToBeClickable(By.xpath("//div[@data-testid='retweetConfirm']")))
    confirmRetweetButton.click()
    Thread.sleep(3000)
    println("Ana gönderi repost edildi!")
  } catch (Exception e) {
    println("Repost işlemi sırasında bir hata oluştu: ${e.message}")
  }
}
