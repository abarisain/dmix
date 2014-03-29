package org.musicpd.android.tools

object YouTube {
	def resolve(idOrUrl: String) = {
            Log.v("Resolving YouTube data " + idOrUrl);
	    val info = scala.io.Source.fromInputStream(
	        new java.net.URL(s"http://www.youtube.com/get_video_info?video_id=${"""[^?]+\?|.*youtu\.be/|.*[/&?]v=|&[^=]*(?<!&v)=[^&]*(&|$)""".r.replaceAllIn(idOrUrl, "")}&asv=2").
	            openConnection().getInputStream()
	    ).mkString("").split("&")
	    Log.v(info.mkString("\n"));
	    for {
	        title <-
                    info.find(_.startsWith("title=")) orElse
	            Some("title=" + """:.*$""".r.replaceAllIn(idOrUrl, ""))
	        urlmap <- info.find(_.startsWith("url_encoded_fmt_stream_map="))
	        streams = java.net.URLDecoder.decode(urlmap.substring(27), "UTF-8").
	            split(",").map(_.split("&"))
            stream <-
            	streams.find(_.exists(_.equals("quality=hd720"))) orElse
            	streams.find(_.exists(_.startsWith("quality=hd"))) orElse
            	streams.find(_.exists(_.startsWith("quality="))) orElse
            	streams.find(!_.isEmpty)
	        url <- stream.find(_.startsWith("url="))
	        sig <- stream.find(_.startsWith("sig=")) orElse Some(null)
	    } yield (
	        java.net.URLDecoder.decode(title.substring(6), "UTF-8"),
	        java.net.URLDecoder.decode(url.substring(4), "UTF-8") + (if (sig == null) "" else "&signature=" + sig.substring(4))
        )}.get
}
