Content with Embedded Images Example

This example downloads the Skyword XML feed and also process any embedded images found
in the article body. Embedded images are <img> references placed within the text/body
of the article content.  These images must be stored on your server.  This example 
stores these images locally and switches the src="" location to the local URL where
the image is stored on your server.

Please see the Skyword XML Feed Integration guide for a complete description.

How to Run:
ant run



