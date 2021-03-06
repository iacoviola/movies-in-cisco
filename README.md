# Movies In Cisco

Movies In Cisco allows you to visualize on a map all the movies which have ever been filmed in the beautiful city of San Francisco, CA, USA.

## How the app works

### STEP ONE
The entire app is based on an open data set made available by the city of San Francisco whose link is the following:

https://data.sfgov.org/Culture-and-Recreation/Film-Locations-in-San-Francisco/yitu-d5am

Starting from this data set which contains, amongst other informations; the location in which a scene of a certain movie was filmed.
The location is not indicated as coordinates but just as an address.
![loading_screen](https://user-images.githubusercontent.com/26089090/168681842-10b88641-e0cc-4c97-89bc-796e2ef69f2b.jpg)

### STEP TWO
This forced me to convert every address for every entry in the data set to coordinates using Google's geocoding API:

https://developers.google.com/maps/documentation/geocoding/overview

The problem with this particular API is that it is restricted to a maximum number of requests per second (50), so i had to configure multiple threads fetch data quickly but looking out for a potential query limit error generated by the web-service, ***the problem still needs to be solved but with an average speed connection it shouldn't occur***.

### STEP THREE
The converted locations are then saved in a local database, to get around the conversion effort every time the app is restarted, and shown on the map as clusters, for performance related reasons. 

![home_screen](https://user-images.githubusercontent.com/26089090/168682086-9c8d9b2e-9b99-4e3b-b907-1d7ac85fd731.jpg)

![clusters](https://user-images.githubusercontent.com/26089090/168681958-5aa21890-92f6-4c41-afb5-e2fe8cc41eef.jpg)

Once a user zooms in, the clusters divide in smaller clusters eventually revealing a marker which can be clicked to show the movie filmed in that location and its poster.

![movie_window](https://user-images.githubusercontent.com/26089090/168681990-fe52d9c0-40b3-4706-97f5-de7c7e45391f.jpg)

If multiple movies where filmed in the same location a cluster will be available to click generating a popup window showing all the different movies.

![cluster_window](https://user-images.githubusercontent.com/26089090/168682002-075d5b65-39e3-4065-899d-73957aac2de6.jpg)

### STEP FOUR
The application is able to load posters for most of the movies thanks to the TMDB API:

https://developers.themoviedb.org/3/getting-started/introduction

Given the name of a movie the web-service responds with a set of information about it, including the path of the poster on the TMDB's database, which can then be retrieved and loaded on the app thanks to the Glide framework.
***This part of the app is a bit rusty since I couldn't put the request function in a separate thread resulting in the UI locking sometimes until the request is completed***.
