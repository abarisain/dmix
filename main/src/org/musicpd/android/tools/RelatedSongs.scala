package org.musicpd.android.tools

import collection.JavaConverters._
import org.a0z.mpd._

import Utils._

object RelatedSongs {
  protected val dir = new scala.util.matching.Regex("""^(.*)/(?:\\/|[^/]*)$""")
  
  def items(mpd : MPD, xs : java.util.List[Music]): java.util.List[Music] = {
    new java.util.ArrayList(items(mpd, xs.asScala).toList.asJava)
  }
  protected def items(mpd : MPD, xs : Seq[Music]) = {
    var selected = xs.toSet
    xs
    //.sortBy(x => x.getDisc() * 1000 + x.getTrack())
    .groupByStable(x => dir.replaceAllIn(x.getFullpath(), "$1"))
    .flatMap(g => mpd.getSongs(g._1).asScala)
    .map(x => if (x == null) null else x.setSelected(selected.contains(x)))
    /*.foldLeft(collection.mutable.LinkedHashSet[Music]())((s,g) =>
      s ++ mpd.getSongs(g._1).asScala
    )*/
  }
}