# Results Similarity Display Requirements

The system will be able to correlate results across sarif files to determine whether they are in fact the same result, even if they've been found from different scan engines.

These similary results will be shown on the Results Detail Page.

## Results Detail Page changes

Update the results detail page to show the similar results.

The similar results can be retrieved by calling this REST API : GET /{id}/results/{resultId}/similarities

This endpoint should NOT be called until required, by expanding the "Similar Results" section.

Examine the endpoint and response entity for more detail.

The similar results should be displayed in a new card between the "Basic Information" card and the "Description" card

The title of the new section should be "Similar Results".
The new section should be collapsible/expandible, collapsed by default.

Upon expanding the area, call the REST API. This will return a list of similarity responses.
If the list is empty, the expanded area should just show "No similar results found"

Where similar results are returned, display them in a table, in the same style as sarifs and results in their respective listing pages.

The fields so be displayed are :
- matchingResultId : Just labelled id, this should be a clickable link to the results detail page for the result
- location
- lineNumber
- ruleId
- similarity : this should be a clickable link, that displays a popup showing the text in the "reason" field. This popup should be closeable.

