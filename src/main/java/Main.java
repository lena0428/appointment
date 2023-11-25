import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.TimeZone;
import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.KeyEvent;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.auth.oauth2.GoogleTokenResponse;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpException;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.File;
import java.io.FileWriter;
import java.security.GeneralSecurityException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class Main {

  private static final String API_KEY = "AIzaSyAvLJY9bWEC0v-Gt9GeGZGfcBNS34Yl3nc";
  private static final String URL = "https://www.googleapis.com/calendar/v3/calendars/daniella950428%40gmail.com/events";

  static HttpTransport HTTP_TRANSPORT;

  static {
    try {
      HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
    } catch (GeneralSecurityException e) {
      throw new RuntimeException(e);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }


    public static void main(String[] args)
        throws IOException, GeneralSecurityException, ScriptException {
      String s = sendGet("https://neu.mywconline.net/schedule2.php");
      System.out.println(decodeHtml(s));
      createICSFile(decodeHtml(s));
    }


//  public static void main(String[] args) throws IOException {
//    String s = sendGet("https://neu.mywconline.net/schedule2.php");
//    List<String> dates = decodeHtml(s);
//    SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMM. d, yyyy, h:mm a");
//    // Set the timezone to Pacific Time
//    dateFormat.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
//    // Get the current year
//    int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR);
//
//    for (String date : dates) {
//      try {
//        String eventTitle = "ITC Tutoring";
//        String eventDescription = "tutor";
//        String[] parts = date.split(": ");
//        String dateStr = parts[0] + ", " + currentYear + ", " + parts[1];
//        Date startDate = dateFormat.parse(dateStr);
//        // Adding 1 hour to start date for the event duration
//        Date endDate = new Date(startDate.getTime() + (60 * 60 * 1000));
//        String script = String.format(
//            "tell application \"Calendar\"\n" +
//                "    tell calendar 1\n" +
//                "        set newEvent to make new event at end with properties {summary:\"%s\", description:\"%s\", start date:(date \"%s\"), end date:(date \"%s\")}\n" +
//                "    end tell\n" +
//                "end tell",
//            eventTitle, eventDescription, startDate, endDate);
//
//
//        String[] cmd = { "osascript", "-e", script };
//        Process process = Runtime.getRuntime().exec(cmd);
//
//        int exitCode = process.waitFor();
//        if (exitCode == 0) {
//          System.out.println("Event added to Calendar successfully.");
//        } else {
//          System.out.println("Failed to add event to Calendar.");
//        }
//      } catch (IOException | InterruptedException e) {
//        e.printStackTrace();
//      } catch (ParseException e) {
//        throw new RuntimeException(e);
//      }
//    }
//  }

  private static List<String> decodeHtml(String s) {
    List<String> res = new ArrayList<>();
    Document doc = Jsoup.parse(s);

    Element appointmentDropdown = doc.selectFirst("a#appointmentDropdown");

    if (appointmentDropdown != null) {
      Elements appointments = appointmentDropdown.nextElementSibling().select("a.dropdown-item");

      if (!appointments.isEmpty()) {
        for (Element appointment : appointments) {
          res.add(appointment.text());
        }
      } else {
        System.out.println("No appointments found.");
      }
    } else {
      System.out.println("My Appointments section not found.");
    }
    System.out.println(res);
    // Define time zones for EST and PT
    ZoneId estZone = ZoneId.of("America/New_York");
    ZoneId ptZone = ZoneId.of("America/Los_Angeles");

    // Custom formatter for the input timestamps with day and comma
    DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder().parseCaseInsensitive()
        .appendPattern("EEEE, MMM. d: h:mm a")
        .parseDefaulting(java.time.temporal.ChronoField.YEAR, LocalDateTime.now().getYear());

    DateTimeFormatter formatter = builder.toFormatter(Locale.US);

    List<String> ptTimestamps = new ArrayList<>();

    for (String timestamp : res) {
      LocalDateTime localDateTime = LocalDateTime.parse(timestamp, formatter);

      // Convert to EST ZonedDateTime
      ZonedDateTime estDateTime = ZonedDateTime.of(localDateTime, estZone);

      // Convert to PT ZonedDateTime
      ZonedDateTime ptDateTime = estDateTime.withZoneSameInstant(ptZone);

      // Format the PT timestamp
      String ptTimestamp = ptDateTime.format(formatter);
      ptTimestamps.add(ptTimestamp);
    }

    return ptTimestamps;
  }

  public static String sendGet(String urlParam) throws IOException {
    // instantiate httpclient
    HttpClient httpClient = new HttpClient();
    // set socket timeout
    httpClient.getHttpConnectionManager().getParams().setConnectionTimeout(15000);
    // initiate get
    GetMethod getMethod = new GetMethod(urlParam);
    // initiate timeout of get method
    getMethod.getParams().setParameter(HttpMethodParams.SO_TIMEOUT, 60000);
    // set request header
    getMethod.addRequestHeader("Accept",
        "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
    getMethod.addRequestHeader("Accept-Language", "en-US,en;q=0.9,zh-CN;q=0.8,zh;q=0.7");
    getMethod.addRequestHeader("Connection", "keep-alive");
    getMethod.addRequestHeader("Cookie",
        "PHPSESSID=38aqkikco93q5it7k4fmmetrl4; WCO_ID=zhu.yidan2%40northeastern.edu%3D889bd53e286d69ade0f075f8548768a2");
    getMethod.addRequestHeader("Connection", "keep-alive");
    getMethod.addRequestHeader("Sec-Fetch-Dest", "document");
    getMethod.addRequestHeader("Sec-Fetch-Mode", "navigate");
    getMethod.addRequestHeader("Sec-Fetch-Site", "none");
    getMethod.addRequestHeader("Sec-Fetch-User", "?1");
    getMethod.addRequestHeader("Upgrade-Insecure-Requests", "1");
    getMethod.addRequestHeader("User-Agent",
        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36");
    getMethod.addRequestHeader("sec-ch-ua",
        "\"Google Chrome\";v=\"119\", \"Chromium\";v=\"119\", \"Not?A_Brand\";v=\"24\"");
    getMethod.addRequestHeader("sec-ch-ua-mobile", "?0");
    getMethod.addRequestHeader("sec-ch-ua-platform", "\"macOS\"");

    httpClient.executeMethod(getMethod);

    return getMethod.getResponseBodyAsString();

  }

  public static void createICSFile(List<String> eventDates) {
    int currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR); // Get the current year

    SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMM. d, yyyy, h:mm a");
    dateFormat.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles")); // Set the timezone to Pacific Time

    StringBuilder icsContent = new StringBuilder();
    icsContent.append("BEGIN:VCALENDAR\n");
    icsContent.append("VERSION:2.0\n");
    icsContent.append("PRODID:-//YourCalendarApp//YourAppVersion//EN\n");

    for (String eventDate : eventDates) {
      try {
        String[] parts = eventDate.split(": ");
        String dateStr = parts[0] + ", " + currentYear + ", " + parts[1];
        Date startDate = dateFormat.parse(dateStr);

        Date endDate = new Date(startDate.getTime() + (60 * 60 * 1000)); // Adding 1 hour to start date for the event duration

        icsContent.append("BEGIN:VEVENT\n");
        icsContent.append("DTSTART:" + formatDate(startDate) + "\n");
        icsContent.append("DTEND:" + formatDate(endDate) + "\n");
        icsContent.append("SUMMARY:Meeting on " + eventDate + "\n");
        icsContent.append("DESCRIPTION:Meeting scheduled for " + eventDate + ".\n");
        icsContent.append("LOCATION:Virtual Meeting\n");
        icsContent.append("END:VEVENT\n");
      } catch (ParseException e) {
        e.printStackTrace();
      }
    }

    icsContent.append("END:VCALENDAR");

    // Write content to a .ics file
    try {
      File file = new File("calendar_events_pacific.ics");
      FileWriter writer = new FileWriter(file);
      writer.write(icsContent.toString());
      writer.close();
      System.out.println("Calendar file created: " + file.getAbsolutePath());
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

      public static void createGoogleCalendar(List<String> eventDates) {
        int currentYear = java.util.Calendar.getInstance()
            .get(java.util.Calendar.YEAR); // Get the current year

        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMM. d, yyyy, h:mm a");
        dateFormat.setTimeZone(
            TimeZone.getTimeZone("America/Los_Angeles")); // Set the timezone to Pacific Time

        for (String eventDate : eventDates) {
          try {
            String[] parts = eventDate.split(": ");
            String dateStr = parts[0] + ", " + currentYear + ", " + parts[1];
            Date startDate = dateFormat.parse(dateStr);

            HttpClient httpClient = new HttpClient();
            PostMethod postMethod = new PostMethod(URL + "?key=" + API_KEY);

            System.out.println(createEventJSON(startDate));
            String eventJSON = createEventJSON(startDate);
            postMethod.setRequestHeader("Content-Type", "application/json");
            postMethod.setRequestBody(eventJSON);

            int statusCode = httpClient.executeMethod(postMethod);
            if (statusCode == HttpStatus.SC_OK) {
              System.out.println("Event inserted successfully for: " + eventDate);
            } else {
              System.out.println("Event insertion failed for: " + eventDate + ", Status code: " + statusCode);
            }
            Date endDate = new Date(
                startDate.getTime() + (60 * 60 * 1000)); // Adding 1 hour to start date for the event duration


          } catch (ParseException e) {
            e.printStackTrace();
          } catch (HttpException e) {
            throw new RuntimeException(e);
          } catch (IOException e) {
            throw new RuntimeException(e);
          }
        }
      }

      private static String createEventJSON(Date date) {
        DateFormat iso8601Format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssXXX");
        iso8601Format.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
        String formattedDate = iso8601Format.format(date);

        return "{" +
            "\"summary\": \"Test Event\"," +
            "\"start\": {" +
            "\"dateTime\": \"" + formattedDate + "\"" +
            "}," +
            "\"end\": {" +
            "\"dateTime\": \"" + formattedDate + "\"" +
            "}" +
            "}";
      }

  //  public static void insertEvent(List<String> eventDates) throws IOException {
  //    // Set up your client ID, secret, redirect URI, and required scopes
  //    String clientId = "279609586817-mgieev1kp21oi2jdkfus16c6tl89dp94.apps.googleusercontent.com";
  //    String clientSecret = "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";
  //    List<String> scopes = Collections.singletonList(
  //        "https://www.googleapis.com/auth/calendar.events");
  //
  //    // Set up the authorization code flow
  //    GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
  //        new NetHttpTransport(), JacksonFactory.getDefaultInstance(), clientId, clientSecret,
  //        scopes).setAccessType("offline")
  //        .setApprovalPrompt("force") // Force to re-prompt for consent
  //        .build();
  //
  //    // Construct the authorization URL
  //    String authorizationUrl = flow.newAuthorizationUrl().build();
  //
  //    // Redirect the user to authorizationUrl to grant access
  //
  //    // Handle the callback to get the authorization code
  //    String authorizationCode = "CODE_FROM_CALLBACK"; // Retrieve the code from the callback URL
  //
  //    // Exchange authorization code for access token and refresh token
  //    GoogleTokenResponse tokenResponse = flow.newTokenRequest(authorizationCode)
  //       .execute();
  //    String accessToken = tokenResponse.getAccessToken();
  //
  //    Credential credential = new GoogleCredential().setAccessToken(accessToken);
  //
  //    Calendar service = new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY,
  //        credential).setApplicationName("Web client 1").build();
  //
  //    for (String date : eventDates) {
  //      Event event = createEvent("ITC", date);
  //      service.events().insert("primary", event).execute();
  //      System.out.println("Event added: " + event.getStart().getDateTime());
  //      service.events().insert("primary", event).execute();
  //    }
  //  }
  //
  //  private static Event createEvent(String summary, String dateTime) {
  //    EventDateTime eventDateTime = new EventDateTime().setDateTime(new DateTime(dateTime))
  //        .setTimeZone("America/Los_Angeles"); // Replace 'Your_Time_Zone' with the appropriate timezone
  //
  //    Event event = new Event().setSummary(summary).setStart(eventDateTime).setEnd(eventDateTime);
  //
  //    return event;
  //  }

  public static String formatDate(Date date) {
    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd'T'HHmmss");
    dateFormat.setTimeZone(
        TimeZone.getTimeZone("America/Los_Angeles")); // Set the timezone to Pacific Time
    return dateFormat.format(date);
  }

//    public static String formatDate(Date date) {
//      SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
//      // Set the timezone to Pacific Time
//      dateFormat.setTimeZone(TimeZone.getTimeZone("America/Los_Angeles"));
//      return dateFormat.format(date);
//    }
}