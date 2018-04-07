## Trailer download tool for Kodi media collections

Downloads trailers for movies, either from a URL defined in the .nfo file, or by searching Youtube and taking the first result.

### Requirements:

A folder structure like this:

    Top-level movie folder
    |> The Hitchhiker's gude to the galaxy (2001)
    |> Alamar (2009)
    |> A.I. Artificial Intelligence (2001)
      |> A.I. Artificial Intelligence (2001) [720p].mp4
      |> A.I. Artificial Intelligence (2001) [720p].nfo
      |> A.I. Artificial Intelligence (2001) [720p]-trailer.mkv
      
The .nfo file must be a standard XML file (in my case, generated by [MediaElch](https://www.kvibes.de/en/mediaelch/)), containing _at least_ the movie title, although the year and a trailer URL will also be used if found.

This program will download the "-trailer" file, if it is not already there.

You'll also need:

* `youtube-dl` installed and in your PATH

* Java (1.8+)

Should work on Windows / Mac / Linux, but I've only tested on Linux.

---

Download the .jar file from the [releases section](https://github.com/JonasCz/KodiTralierDownloader/releases/), and run it:

    java -jar KodiTralierDownloader.jar --input-directory "/path/to/top-level-directory/"
    
 It will (should...) do the rest (That is, download a trailer for each movie in each subdirectory of top-level-directory). I'd recommend piping the output to a file so you can check that the correct trailers were downloaded, since the youtube search for "<moviename> <year> trailer" doesn't always actually give a proper (or good) trailer, especially for more obscure movies.
 
 ---
 
  I recommend not looking at the code, it's a mess and will hurt your eyes (and brain).
 
 You're welcome to report bugs, but it's unlikely I'll fix them, since this is an "I wrote this in an afternoon because I didn't find another way to do it, and it works for me" kind of project.
