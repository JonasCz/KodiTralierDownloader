package com.jonascz;

import com.sapher.youtubedl.*;
import me.tongfei.progressbar.ProgressBar;
import org.apache.commons.cli.*;
import org.apache.commons.io.FileUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;

import java.io.File;
import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        Options cliOptions = new Options();
        cliOptions.addOption("d", "input-directory", true, "Top level directory to process, expects each movie to be in an individual sub-directory. Full path, or defaults to current directory if this option is not given.");
        cliOptions.addOption("s", "skip-download", false, "Don't actually download anything, just print output. Passes \"--skip-download\" to youtube-dl");

        CommandLine commandLine = null;

        try {
            commandLine = new DefaultParser().parse(cliOptions, args);
        } catch (ParseException e) {
            System.err.println("Error parsing commandline arguments");
            e.printStackTrace();
        }

        //set the top level dir to the user-supplied one, default to current working directory if it isn't supplied.
        File topLevelDir = new File(commandLine.getOptionValue("d", System.getProperty("user.dir")));

        if (commandLine.hasOption("d")) {
            System.out.println("Top-level movie directory set from commandline argument: " + topLevelDir.getAbsolutePath());
        } else {
            System.out.println("Top-level movie directory set to current directory: " + topLevelDir.getAbsolutePath());
        }

        for (File movieDirectory : topLevelDir.listFiles()) {
            //find the .nfo XML file
            boolean alreadyHasTrailer = false;

            if (movieDirectory.isDirectory()) {
                System.out.println("Processing: " + movieDirectory.getName());
            } else {
                System.out.println(movieDirectory.getName() + " is not a directory, skipping.");
                continue;
            }

            //is there already a trailer ?
            for (File f: movieDirectory.listFiles()) {
                if (f.getName().contains("-trailer") && !f.getName().endsWith(".part")) {
                    System.out.println("Skipping, There's already a trailer file here: " + f.getName());
                    System.out.println();
                    alreadyHasTrailer = true;
                }
            }

            //this seems kludgy, surely there's a more elegant way ?
            if (alreadyHasTrailer == true) continue;

            File nfoFile = null;
            for (File f: movieDirectory.listFiles()) {
                if (f != null && f.getName().endsWith(".nfo")) {
                    nfoFile = f;
                    break;
                }
            }

            Document document = null;

            try {
                if (nfoFile == null || !nfoFile.exists()) {
                    System.out.println("|> No .nfo file found");
                    System.out.println();
                    continue;
                } else {
                    document = Jsoup.parse(FileUtils.readFileToString(nfoFile), "", Parser.xmlParser());
                }
            } catch (IOException e) {
                System.out.println("|> Error reading .nfo file");
                System.out.println();
                e.printStackTrace();
                continue;
            }

            System.out.println("|> Loaded XML from file: " + nfoFile.getName());

            if (document.select("movie > trailer").size() > 0 && document.select("movie > trailer").first().text().length() != 0) {
                String trailerUrl = document.select("movie > trailer").first().text();
                System.out.println("|> Trailer URL found in .nfo file: " + trailerUrl);

                YoutubeDLRequest request = new YoutubeDLRequest(trailerUrl, movieDirectory.getAbsolutePath() + File.separator);
                request.setOption("--output", nfoFile.getName().substring(0, nfoFile.getName().length() - 4) + "-trailer.%(ext)s");
                request.setOption("--max-downloads", 1);
                request.setOption("--match-filter", "duration <= 360"); //don't get videos linger than 6 mins, avids getting "full movie" videos

                if (commandLine.hasOption("s")) {
                    request.setOption("--skip-download");
                }

                YoutubeDLResponse response = null;

                System.out.println("|> Starting Download for \"" + getVideoTitle(trailerUrl)  +  "\" (" + trailerUrl + ")" );

                ProgressBar pb = new ProgressBar("Download", 100).start();

                try {
                     response = YoutubeDL.execute(request, new DownloadProgressCallback() {
                         @Override
                         public void onProgressUpdate(float progress, long etaInSeconds) {
                             pb.stepTo((long) progress);
                         }
                     });

                } catch (YoutubeDLException e) {
                    e.printStackTrace();
                }

                pb.stop();

                System.out.println("|> Download finished, here's the output from youtube-dl:");
                System.out.println(response.getOut());
                System.out.println();



            } else {
                StringBuilder ytSearchStringBuilder = new StringBuilder();

                ytSearchStringBuilder.append(document.select("movie > title").first().text());

                //if we have a year, ad it to the search string.
                if (document.select("movie > year").size() > 0 && document.select("movie > year").first().text().length() != 0) {
                    ytSearchStringBuilder.append(" ");
                    ytSearchStringBuilder.append(document.select("movie > year").first().text());
                }

                ytSearchStringBuilder.append(" trailer");

                System.out.println("|> No trailer URL found in .nfo file, searching YouTube for: \"" + ytSearchStringBuilder.toString() + "\" and downloading the first result.");

                try {
                    String url = getVideoUrl(ytSearchStringBuilder.toString());
                    YoutubeDLRequest request = new YoutubeDLRequest(url, movieDirectory.getAbsolutePath() + File.separator);
                    request.setOption("--output", nfoFile.getName().substring(0, nfoFile.getName().length() - 4) + "-trailer.%(ext)s");
                    request.setOption("--max-downloads", 1);
                    request.setOption("--match-filter", "duration <= 360"); //don't get videos linger than 6 mins, avids getting "full movie" videos

                    if (commandLine.hasOption("s")) {
                        request.setOption("--skip-download");
                    }

                    YoutubeDLResponse response = null;

                    System.out.println("|> Starting Download for \"" + getVideoTitle(url) + "\" (" + url + ")");

                    ProgressBar pb = new ProgressBar("Download", 100).start();

                    response = YoutubeDL.execute(request, new DownloadProgressCallback() {
                        @Override
                        public void onProgressUpdate(float progress, long etaInSeconds) {
                            pb.stepTo((long) progress);
                        }
                    });

                    pb.stop();


                    System.out.println("|> Download finished, here's the output from youtube-dl:");
                    System.out.println(response.getOut());
                    System.out.println();

                } catch (YoutubeDLException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static String getVideoUrl (String searchQuery) throws YoutubeDLException {
        for (int i = 1;; i++) {
            YoutubeDLRequest request = new YoutubeDLRequest("ytsearch10:" + searchQuery, System.getProperty("user.dir"));
            //request.setOption("--get-url");
            request.setOption("--match-filter", "duration <= 360"); //don't get videos linger than 6 mins, avids getting "full movie" videos
            request.setOption("--playlist-items", i);
            request.setOption("--skip-download");

            String out = YoutubeDL.execute(request).getOut();

            if (out.contains("does not pass filter")) {
                continue;
            } else {
                request = new YoutubeDLRequest("ytsearch10:" + searchQuery, System.getProperty("user.dir"));
                //request.setOption("--get-url");
                request.setOption("--match-filter", "duration <= 360"); //don't get videos linger than 6 mins, avids getting "full movie" videos
                request.setOption("--playlist-items", i);
                request.setOption("--max-downloads", 1);
                request.setOption("--get-id");

                out = "https://www.youtube.com/watch?v=" + YoutubeDL.execute(request).getOut().trim();
                return out;
            }
        }
    }

    public static String getVideoTitle (String url) {
        YoutubeDLRequest request = new YoutubeDLRequest(url, System.getProperty("user.dir"));
        request.setOption("--get-title");
        request.setOption("--match-filter", "duration <= 360"); //don't get videos linger than 6 mins, avids getting "full movie" videos
        request.setOption("--max-downloads", 1);

        try {
            return YoutubeDL.execute(request).getOut().trim();
        } catch (YoutubeDLException e) {
            e.printStackTrace();
            return "<no title found>";
        }
    }
}
