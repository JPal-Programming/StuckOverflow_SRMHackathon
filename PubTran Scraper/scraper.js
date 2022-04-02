//////////////////////// REMOVE BEFORE PUSHING ////////////////////////
const API_KEY;
const PATH_TO_SERVICE_ACCOUNT;
///////////////////////////////////////////////////////////////////////

const puppeteer = require("puppeteer");

var axios = require("axios");

const admin = require("firebase-admin");
const serviceAccount = require(PATH_TO_SERVICE_ACCOUNT);
const { GeoPoint, Timestamp } = require("@google-cloud/firestore");

const vehicleName = "BNSF Chicago Suburb";

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
});

let db = admin.firestore();

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

  for (const link of links) {
    var arr = link.split(".");
    if (arr[arr.length - 1] === "pdf") {
      links.splice(links.indexOf(link));
    }
  }

  for (const link of links) {
    console.log("Searching Link: " + link);

    const page2 = await browser.newPage();

    await page2.goto(link);
    const element = await findByLink(page2, "Aurora");

    if (element != null) {
      const stopNames = await getStopNames(page2, element);
      await processStops(stopNames);
    }
  }
  await closeBrowser(browser);
}

async function processStops(stopNames) {
  let stopsProcessed = [];
  for (const stopName of stopNames) {
    const tester = new RegExp("[A-Z]");
    if (tester.test(stopName[0])) {
      stopsProcessed.push(stopName);
    } else {
      stopNames.splice(stopNames.indexOf(stopName), 1);
    }
  }
  let uniqueStops = [...new Set(stopsProcessed)];
  await checkLength(uniqueStops);
}

async function checkLength(stops) {
  const doc = await db
    .collection("routes")
    .doc("BNSF Chicago Subdivision")
    .get();
  if (doc.exists) {
    const numStops = doc.data().stopsAll.length;

    console.log(doc.data().stopsAll);
    const currentStopNames = doc.data().stopsAll;

    if (stops.length > numStops) {
      eliminateDuplicates(stops, currentStopNames);
    }
  }
}

function eliminateDuplicates(stops, currentStopNames) {
  let uniqueStops = stops.filter((el) => !currentStopNames.includes(el));
  getLocationData(uniqueStops);
}

async function getLocationData(stops) {
  let locations = [];
  for (let i = 0; i < stops.length; i++) {
    var config = {
      method: "get",
      url:
        "https://maps.googleapis.com/maps/api/place/findplacefromtext/json?fields=formatted_address&input=" +
        encodeURI(vehicleName + " " + stops[i] + " station") +
        "&inputtype=textquery&key=" + API_KEY,
      headers: {},
    };

    const response = await axios(config);
    console.log(
      stops[i] +
        ": " +
        JSON.stringify(response.data.candidates[0].formatted_address)
    );
    locations.push(response.data.candidates[0].formatted_address);
  }
  console.log(locations);
  getPreciseLocations(stops, locations);
}

async function getPreciseLocations(stops, locations) {
  let coordinates = [];
  for (let i = 0; i < locations.length; i++) {
    var config = {
      method: "get",
      url:
        "https://maps.googleapis.com/maps/api/geocode/json?address=" +
        encodeURI(locations[i]) +
        "&key=" + API_KEY,
      headers: {},
    };

    const response = await axios(config);
    coordinates.push(response.data.results[0].geometry.location);
  }
  writeToServer(coordinates, stops);
}

async function writeToServer(locations, stopNames) {
  console.log("Writing to server");
  const doc = await db
    .collection("routes")
    .doc("BNSF Chicago Subdivision")
    .get();
  let currentStops = doc.data().stops;
  let stopsAll = doc.data().stopsAll;
  const currentPos = doc.data().currentPos;

  const currentInd = getCurrentIndex(currentPos, locations);

  for (let i = 0; i < locations.length; i++) {
    let stop = {};
    stop.pos = new GeoPoint(locations[i].lat, locations[i].lng);

    if (i <= currentInd) {
      let d = new Date();
      d.setTime(0);
      stop.arr = Timestamp.fromDate(d);
      stop.dep = Timestamp.fromDate(d);
      stop.travelTime = 0;
    }

    const speed = doc.data().speed;
    const travelTime =
      (getDistance(
        currentPos.latitude,
        currentPos.longitude,
        locations[i].lat,
        locations[i].lng
      ) /
        speed) *
      60; //convert to minutes
    stop.travelTime = Math.round(travelTime);

    let currentMillis = Date.now();
    currentMillis += travelTime * 60 * 1000;

    let d = new Date();
    d.setTime(currentMillis);
    stop.arr = Timestamp.fromDate(d);
    stop.dep = Timestamp.fromDate(d);

    currentStops[stopNames[i]] = stop;
    if (!stopsAll.includes(stopNames[i])) stopsAll.push(stopNames[i]);
  }

  const res = await db
    .collection("routes")
    .doc("BNSF Chicago Subdivision")
    .update({ stops: currentStops, stopsAll: stopsAll });

  console.log("Stop successfully updated");
}

function getCurrentIndex(currentPos, locations) {
  console.log(locations);
  let minInd = 0;
  let minDist = getDistance(
    currentPos.latitude,
    currentPos.longitude,
    locations[0].lat,
    locations[0].lng
  );

  for (let i = 0; i < locations.length; i++) {
    if (
      getDistance(
        currentPos.latitude,
        currentPos.longitude,
        locations[i].lat,
        locations[i].lng
      ) < minDist
    ) {
      minInd = i;
      minDist = getDistance(
        currentPos.latitude,
        currentPos.longitude,
        locations[i].lat,
        locations[i].lng
      );
    }
  }

  if (
    (minInd =
      locations.length ||
      getDistance(
        currentPos.latitude,
        currentPos.longitude,
        locations[minInd - 1].lat,
        locations[minInd - 1].lng
      ) >
        getDistance(
          currentPos.latitude,
          currentPos.longitude,
          locations[minInd + 1].lat,
          locations[minInd + 1].lng
        ))
  ) {
    return minInd;
  }
  return minInd - 1;
}

// Unit: miles
function getDistance(lat1, lon1, lat2, lon2) {
  if (lat1 == lat2 && lon1 == lon2) {
    return 0;
  } else {
    var radlat1 = (Math.PI * lat1) / 180;
    var radlat2 = (Math.PI * lat2) / 180;
    var theta = lon1 - lon2;
    var radtheta = (Math.PI * theta) / 180;
    var dist =
      Math.sin(radlat1) * Math.sin(radlat2) +
      Math.cos(radlat1) * Math.cos(radlat2) * Math.cos(radtheta);
    if (dist > 1) {
      dist = 1;
    }
    dist = Math.acos(dist);
    dist = (dist * 180) / Math.PI;
    dist = dist * 60 * 1.1515;
    return dist;
  }
}

async function getStopNames(page, element) {
  const parent = (await element.$x(".."))[0];
  console.log(parent._remoteObject.description);
  const elements = await page.$$(
    parent._remoteObject.description + " " + element._remoteObject.description
  );
  let stopNames = [];
  for (var i = 0; i < elements.length; i++) {
    let valueHandle = await elements[i].getProperty("innerText");
    let linkText = await valueHandle.jsonValue();
    const text = getText(linkText);
    stopNames.push(text);
    console.log(text);
  }
  return stopNames;
}

(async () => {
  await getSearchResults(vehicleName);
  process.exit(1);
})();
