const puppeteer = require("puppeteer");

async function scrape() {
  const browser = await puppeteer.launch({});
  const page = await browser.newPage();

  const vehicleName = "bnsf chicago subdivision";

  await page.goto(
    "https://www.google.com/search?q=" + encodeURI(vehicleName + " schedule")
  );

  // Gets 1st page of information
  const searchResults = await page.evaluate(() =>
    [...document.querySelectorAll(".LC20lb")].map((e) => ({
      title: e.innerText,
      link: e.parentNode.href,
    }))
  );
  
  for (let searchResult of searchResults) {
    let link = searchResult[1];
    await page.goto(link);
    const elements = page.$$('');
    for (var i=0; i < elements.length; i++) {
        let valueHandle = await elements[i].getProperty('innerText');
        let linkText = await valueHandle.jsonValue();
        const text = getText(linkText);
        if (linkString == "Aurora") {
          console.log(elements[i]);
        }
    }
  }

  browser.close();
}
scrape();
