# Sherwood UI Requirements

This document outlines the requirements for the UI.

Examine the sherwod-server module also, to make you the REST APIs and entities are there in order to build out the UI Correctly.

## Basic Technical Direction

- Build a single page app using React UI.
- Pick a suitable CSS Framework, I like material design.
- The UI should have a suitable icons library integrated, like font-awesome.
- The main React Component should be mounted to static/index.html
- The Application needs to make a number of REST Api calls.
- The Application should have a light and dark theme with the ability to easily toggle them.
- The application must be built into the static/app folder for serving from my Caddy server.

## Basic Functional Requirements

The application will have 3 main pages

- A page for displaying a list of all sarifs in the system
- A page for displaying the results contained within each sarif
- A page for viewing the details of each result
- For all REST Api calls, assume a relative path e.g. /api/sherwood/sarifs

### Sarif Display Page

This will be the default homepage for the application, accessible at the route of the app .e.g. if running the app on port 14080, this page will be accessible at http://localhost:14080/

This page will show a basically list of all sarifs, accessible via the REST API call to GET /api/sherwood/sarifs

All fields should be displayed.
The table should have a subtle highlighting of alternating rows.

It should be possible to sort the table by vendor or repository or created.

There should also be the ability to upload a sarif, via a pop accessible via an "upload" button on the page.

This should allow the user to pick 1 file and uploaded it via a POST to /api/sherwood/sarifs. Upon a successful upload, the table should refresh to show the new sarif.

The ID field of the sarif should be a clickable link which will bring you to the results page for that sarif.

At all times, any navigation should update the URL so that refreshing the page will bring you to the correct page.

### Results Display Page

The URL for this page should be /sarifs/{sarifId}/results

The data for this page is accessible via the REST API: GET /api/sherwood/sarifs/{sarifId}/results

Same styling for the results as the sarif page.

The following columns should be displayed :
- id
- location : sortable
- line_number
- rule_id
- confidence : sortable
- reachable : sortable
- created : sortable
- updated : sortable

The ID field should be a clickable link that takes you into the result detail page

### Result Detail Page

The URL for this page should be /sarifs/{sarifId}/results/{resultId}

The data for this page is accessible via the REST API: GET /api/sherwood/sarifs/{sarifId}/results/{resultId}

The detail page should show the following fields :

- id
- sarif : This is an id, this should be a clickable link that takes you to the sarif results page
- location
- line_number
- snippet : This will be text containing source code, it should be formatted appropriately with syntax highlighting etc.
- decription : This may be quite long, so can be wrapped
- rule_id
- confidence
- reachable
- graph : This is a base64 string that contains a mermaid diagram for the code paths for this result. This needs to be base64 decoded and use the resulting text to render the mermaid diagram for the text.
- created
- updated