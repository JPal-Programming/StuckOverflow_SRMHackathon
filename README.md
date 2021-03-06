
# PubTran

In a world of consistent and reliable automation, the transportation industry has had a technological stagnation. It is often very difficult to get definitive information concerning the location and expected time of arrival of trains, buses, and subways without the governing transit authority making an active effort to enable their vehicles with up-to-date technology. So one tool that has the potential of revolutionizing public transportation is a peer-based platform that allows the collection of vehicle information dispersed throughout the various modes of public transportation. My platform allows people on buses or trains to share their location, thereby uploading accurate location updates for the duration of their journey. This location information is then used to construct detailed routes of the bus and train lines in a given city. For someone who is waiting at a bus stop, they can then see the current location of their bus by referencing the location of another person already on that bus. 

The app is based on the distribution of data sourced from individual users. When a user opens the app, they will either have the option to view the buses and trains in their area or indicate that they are currently on a public transit vehicle. If they choose to share their location, they will then choose from a list of possible buses and trains that operate in the general vicinity of their current location. From there, they will provide location updates for that vehicle until they exit it, at which point they will indicate to the app that they are no longer on the vehicle. If another user at the same time chose instead to view the vehicles in their area, they would see the location updates provided by the first user and get up-to-date information about it, including direction, speed, location, and the calculated ETA based on all three previous data points. Much like BitTorrent, this app will allow members of the public to help each other, with the general health and scalability of the app reliant on the support of those who choose to use it.


## Tech Stack

**Front-end Client:** Android Mobile

The main function of the mobile client is to serve as the user interface. It can update the location of vehicles stored in the server and display the vehicles close to the user.

**Back-end Server:** Firebase Firestore Cloud Database

The Firestore database stores all data sent by the front-end and middle-end clients. I chose Firebase because it was especially easy to implement with NodeJS and Android compared to other cloud database solutions.

**Middle-end:** NodeJS Web Scraper

The NodeJS script has multiple functions:

1. Scraping the web for data about the stops:

   * The client searches for the vehicle name, along with several other keywords to optimize search results.
   * The top search results are scraped for stop names
   * The Google Places and GeoCache APIs are used to get precise locations for each stop.
   * The stops are added to the database along with estimated times of arrival, etc.
  
2. Optimizing estimated times of arrival:
  
    * The speed based on timestamps and geoposition is calculated.
    * This is used to calculate and update ETAs




## Notice - IMPORTANT

The API keys have been removed for security. If you would like to request the API keys for demo purposes, [please reach out to me by email.](mailto:bbobjoeyguy@gmail.com)
