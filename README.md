# Short Polling and Long Polling Example in Java with Embedded Jetty

This project demonstrates simple examples of **Short Polling** and **Long Polling** implemented in Java using an embedded Jetty server. It simulates a data source that updates periodically and exposes two HTTP endpoints allowing clients to poll for new data.

---

## Overview

### Short Polling
- The client repeatedly sends HTTP requests at fixed intervals to check if new data is available.
- The server responds **immediately** with either new data (if available) or `null` if there is no update.
- This approach is simple but can generate many unnecessary requests when data changes infrequently.

### Long Polling
- The client sends a request to the server which **holds the request open** until new data is available or a timeout occurs.
- If new data arrives within the timeout, the server responds immediately.
- If no data arrives before timeout, the server responds with `null`, prompting the client to send another request.
- This reduces the number of requests compared to short polling and provides near real-time updates.

---

## Project Structure

- `PollingServer.java` - Main Java class that:
  - Starts an embedded Jetty server on port `8080`.
  - Contains the shared simulated data store.
  - Hosts two servlet endpoints:
    - `/shortpoll` - handles short polling requests.
    - `/longpoll` - handles long polling requests.
  - Runs a background thread that updates the shared data every 10 seconds.

---

## API Endpoints

### 1. Short Polling

- **URL:** `/shortpoll`
- **Method:** GET
- **Query Parameter:** `lastSeenId` (integer) — the last data version the client saw.
- **Behavior:**
  - Immediately returns the new data if available (`id > lastSeenId`).
  - Otherwise returns `{ "newData": null }`.
  

## Requirements and Setup
Java 17 (or compatible Java version).
Maven for dependency management (or include Jetty and Jakarta Servlet jars manually).
Embed Jetty version 11.x compatible with Jakarta Servlet API 5.0.
Running the Server

## Build the project with Maven:
mvn clean package
**Run the server class:**
java -cp target/polling-server-1.0-SNAPSHOT.jar PollingServer

## Access the endpoints on:

http://localhost:5000/shortpoll
http://localhost:5000/longpoll
