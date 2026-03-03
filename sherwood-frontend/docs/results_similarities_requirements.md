API Has been updated to this format

GET /api/sherwood/sarifs/c3f201ec-56a2-484d-a5a7-cf8b54ce9123/similarities

[
  {
    "matchingResultId": "61a8fabd-29f8-43ef-92a8-14a626b55ec6",
    "sarifId": "a2d5ac00-8540-4f44-a7e3-d8c6a2119774",
    "similarity": {
      "availableScore": 2.014786128407962,
      "totalScore": 2.8207005797711466,
      "reasons": [
        {
          "title": "Rule Embedding Similarity",
          "score": 0.834536676867985,
          "weight": 0.4,
          "available": true,
          "additionalInformation": ""
        }
      ]
    },
    "vendor": "Opengrep OSS"
  }
]

Results Detail Page Changes

The similarity score is not returned in an object.
The field to display now is similarity.totalScore

Clicking on the similarity percentage score, used to show a popup with the reason string.

This should now display a table list each item in the reasons array.
The colums to display are "Title", "Score", "Weight", "Additional Information"

Below the table, display the availableScore and totalScore values, with labels "Available Score", "Total Score"

These changes should also apply to the popup displayed when clicking the compare link.

The area where similarity scrore and reason is displayed should be replaced with this list of reasons, along with the available score and total score.