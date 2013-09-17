Contributing to maps-app-android
=================================

 1. [Getting Involved](#getting-involved)
 2. [Reporting Bugs](#reporting-bugs)
 3. [Contributing Code](#contributing-code)

## Getting Involved

Third-party patches are absolutely essential on our quest to create the best maps app on android.
However, they're not the only way to get involved with the development of maps-app-android.
You can help the project tremendously by discovering and [reporting bugs](#reporting-bugs),
[improving documentation](#improving-documentation),
helping others with [GitHub issues](https://github.com/Esri/maps-app-android/issues),
tweeting to [@ArcGIS_Runtime](https://twitter.com/ArcGIS_Runtime),
and spreading the word about mapps-app-android and the [ArcGIS Runtime SDK for Android](https://developers.arcgis.com/en/android/) among your colleagues and friends.

## Reporting Bugs

Before reporting a bug on the project's [issues page](https://github.com/Esri/maps-app-android/issues),
first make sure that your issue is caused by maps-app-android, not your application code.
Second, search through the reported issues for your issue,
and if it's already reported, just add any additional details in the comments.

After you made sure that you've found a new maps-app-android bug,
here are some tips for creating a helpful report that will make fixing it much easier and quicker:

 * Be very clear about your issue in the title. 
 * Include **hardware & android api version** info in the description.
 * Offer plenty of details to help others understand the problem.
 * If possible, create a **simple reproducible test case** to demonstrates the bug.

## Contributing Code
### System Requirements
System requirements of the Android SDK are provided [here](https://developers.arcgis.com/en/android/system-reqs.html).  Below are the basic developer requirements to workign with ArcGIS Runtime SDK For Android in Eclipse: 

 * [JDK 6](http://www.oracle.com/technetwork/java/javase/downloads/index.html)
 * [Eclipse 3.7](http://www.eclipse.org/downloads/) (Indigo) or greater
 * Eclipse [JDT](http://www.eclipse.org/jdt/) plugin (Included in most Eclipse IDE packages)
 * Eclipse [EGit](http://www.eclipse.org/egit/) plugin (Included in Ecilpse 4.x and greater)
 * Android [ADT Plugin](http://developer.android.com/tools/sdk/eclipse-adt.html)
 * [ArcGIS Runtime SDK for Android](https://developers.arcgis.com/en/android/)

### Setting up your dev environment
Install and setup your Eclipse developer environment [here](https://developers.arcgis.com/en/android/install.html).Once installed you can work with the project directly 

#### Fork the repo
If you haven't already, got to https://github.com/Esri/maps-app-android and click the [Fork](https://github.com/Esri/maps-app-android/fork) button.

#### Clone the repo
Open your terminal, navigate to your working folder of choice, and use ```git clone``` to get a copy of the repo you just forked.


```
$ git clone https://github.com/username/maps-app-android.git
# Clones your fork of the repository into the current directory in terminal
```
#### Configure remotes
Move into the directory the cloning process just created (should be maps-app-android), then make sure your local git knows about all the remotes and branches.

```
$ cd maps-app-android
# Changes the active directory in the prompt to the newly cloned "maps-app-android" directory
$ git remote add upstream https://github.com/Esri/maps-app-android.git
# Assigns the original repository to a remote called "upstream"
$ git fetch upstream
# Pulls in changes not present in your local repository, without modifying your files
```

#### Import Project into Eclipse
 * Open up Eclipse Git Repository Viewing Perspective
 * Expand the working directory to find the maps-app folder
 * Right click the maps-app project folder and select **Import Projects**
 * Ensure your project is selected and the **Import existing projects** radio button is selected and click Next
 * Inspect the Import Projects dialog to ensure your project is selected and click **Finish**
 * Change perspectives to **Java**
 * Right click the project **ArcGIS Tools > Convert to ArcGIS Project**
 * You can now work with the project as you would normally.
