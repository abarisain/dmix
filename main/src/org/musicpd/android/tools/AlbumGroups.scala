package org.musicpd.android.tools

import collection.JavaConverters._
import org.a0z.mpd._

import Utils._

class AlbumGroup(display : String, as : List[Album]) extends Album(display) {
  protected val catalogue = new scala.util.matching.Regex("""^(.*?)(?:,? +[(]([^()]+)[)])$""")
  def albums() = new java.util.ArrayList(as.map(a => new Album(catalogue.replaceAllIn(a.getName(), "$2, $1"))).toList.asJava)
  def get(i : Int) = as(i)
}

object AlbumGroups {
  protected val recordings = new scala.util.matching.Regex("""^(.+?)(?i: music| works?| pieces?)?(?:, +[IVXivx\d/:-]*[IVXivx/:-][IVXivxa\d/:-]*\b|,? *\b(?:[Vv]ol\.?|No\.?|[Oo]p(?:\.|us)?|Book|BB|Sz\.?) +[IVXivxa\d/:-]+(.*?))*(?:,? +[(]([^()]+)[)])*$""")
  protected val punct = new scala.util.matching.Regex("""(?:\W*[(][^()]+[)])*\W+""");
  protected val numbers = new scala.util.matching.Regex("""\b(?:(0|zero|zero)|(1|one|un)|(2|two|deux)|(3|three|trois)|(4|four|quatre)|(5|five|cinq)|(6|six|six)|(7|seven|sept)|(8|eight|huit)|(9|nine|neuf)|(10|ten|dix)|(11|eleven|onze)|(12|twelve|douze)|(13|thirteen|treize)|(14|fourteen|quatorze)|(15|fifteen|quinze)|(16|sixteen|seize)|(17|seventeen|dix-sept)|(18|eighteen|dix-huit)|(19|nineteen|dix-neuf)|(?:(20|twenty|vingt)|(30|thirty|trente)|(40|fourty|quarante)|(50|fifty|cinquante)|(60|sixty|soixante)|(70|seventy|soixante-dix)|(80|eighty|quatre-vingts)|(90|ninety|quatre-vingt-dix))(?: (?:(0|zero|zero)|(1|one|(?:et )?un)|(2|two|deux)|(3|three|trois)|(4|four|quatre)|(5|five|cinq)|(6|six|six)|(7|seven|sept)|(8|eight|huit)|(9|nine|neuf)|(10|ten|dix)|(11|eleven|onze)))?|(\d+))\b""".replace(" ", "\\W+"))
  protected val number_initial = new scala.util.matching.Regex("""\A(?:(0|zero|zero)|(1|one|un)|(2|two|deux)|(3|three|trois)|(4|four|quatre)|(5|five|cinq)|(6|six|six)|(7|seven|sept)|(8|eight|huit)|(9|nine|neuf)|(10|ten|dix)|(11|eleven|onze)|(12|twelve|douze)|(13|thirteen|treize)|(14|fourteen|quatorze)|(15|fifteen|quinze)|(16|sixteen|seize)|(17|seventeen|dix-sept)|(18|eighteen|dix-huit)|(19|nineteen|dix-neuf)|(?:(20|twenty|vingt)|(30|thirty|trente)|(40|fourty|quarante)|(50|fifty|cinquante)|(60|sixty|soixante)|(70|seventy|soixante-dix)|(80|eighty|quatre-vingts)|(90|ninety|quatre-vingt-dix))(?: (?:(0|zero|zero)|(1|one|(?:et )?un)|(2|two|deux)|(3|three|trois)|(4|four|quatre)|(5|five|cinq)|(6|six|six)|(7|seven|sept)|(8|eight|huit)|(9|nine|neuf)|(10|ten|dix)|(11|eleven|onze)))?|(\d+))\b""".replace(" ", "\\W+"))
  def number_chuck(x : String) = if (x == null || x.isEmpty() || !x(0).isDigit) x else "ZZ"+x
  def items(xs : java.lang.Iterable[Album], long : Boolean = true): java.util.List[Album] = {
    new java.util.ArrayList(items(xs.asScala, long).toList.asJava)
  }
  protected def items(xs : Traversable[Album], long : Boolean) = {
    //val num = if (long) number_initial else numbers
    val num =
     if (long)
       number_chuck _
     else
	  numbers.replaceAllIn(_ : String, x => {
	    var ns = for (
	      i <- (1 to x.groupCount) if x.group(i) != null;
	      n = if(i < 22) i - 1 else if (i < 29) 20 + (i - 21) * 20 else if (i < 41) i - 29 else x.group(i).toInt
	    ) yield n
	    ns.sum.formatted("~%04d")
	  })
	xs
	.map(x => (recordings.replaceAllIn(x.getName(), m => m.group(1)?:"" + m.group(2)?:"" + " ..."), x))
	.groupByOrdered(x => num(punct.replaceAllIn(x._1, " ").toLowerCase()))
	.map(g => {
	  val xs = g._2
	  val x = xs.head
	  val _xs = xs.tail
	  if (_xs.isEmpty)
	    x._2
	  else
		new AlbumGroup(x._1, xs.map(_._2).toList)
	})
  }
}