const { link } = require("fs");
const puppeteer = require("puppeteer");

// Boilerplate stuff
async function startBrowser() {
  const browser = await puppeteer.launch();
  const page = await browser.newPage();
  return { browser, page };
}

async function closeBrowser(browser) {
  return browser.close();
}

// Normalizing the text
function getText(linkText) {
  linkText = linkText.replace(/\r\n|\r/g, "\n");
  linkText = linkText.replace(/\ +/g, " ");

  // Replace &nbsp; with a space
  var nbspPattern = new RegExp(String.fromCharCode(160), "g");
  return linkText.replace(nbspPattern, " ");
}

// find the link, by going over all links on the page
async function findByLink(page, linkString) {
  const elements = await page.$$("option, a, h1, h2, h3, h4, h5, h6, p");
  for (var i = 0; i < elements.length; i++) {
    let valueHandle = await elements[i].getProperty("innerText");
    let linkText = await valueHandle.jsonValue();
    const text = getText(linkText);
    if (linkString == text) {
      return elements[i];
    }
  }
  return null;
}

async function getSearchResults(name) {
  const { browser, page } = await startBrowser();
  page.setViewport({ width: 1366, height: 768 });

  await page.goto(
    "https://www.google.com/search?q=" + encodeURI(name + " schedule")
  );
  const results = await page.$x("//div[@class = 'g']//a[h3]");
  const links = await page.evaluate(
    (...results) => results.map((link) => link.href),
    ...results
  );

  for (const link of links.slice(0, 5)) {
    // Top 5 results
    console.log("Searching Link...");

    const page2 = await browser.newPage();

    await page2.goto(link);
    const element = await findByLink(page2, "Aurora");

    if (element != null) {
      console.log(element._remoteObject.description);
      await getStopNames(page2, element);
    }
  }
  await closeBrowser(browser);
}

async function getStopNames(page, element) {
  const parent = (await element.$x(".."))[0];
  console.log(parent._remoteObject.description);
  const elements = await page.$$(
    parent._remoteObject.description + " " + element._remoteObject.description
  );
  for (var i = 0; i < elements.length; i++) {
    let valueHandle = await elements[i].getProperty("innerText");
    let linkText = await valueHandle.jsonValue();
    const text = getText(linkText);
    console.log(text);
  }
}

(async () => {
  await getSearchResults("bnsf chicago suburb");
  process.exit(1);
})();
