package com.namelessdev.mpdroid.tools

import collection.JavaConverters._
import org.a0z.mpd._

object Utils {
	implicit def elvisOperator[T](alt: =>T) = new {
		def ?:[A >: T](pred: A) = if (pred == null) alt else pred
	}
	def mapMaybe[A,B](list: List[A])(f: A => B): Traversable[B] = {
		mapMaybe(list.view)(f)
	}
	def mapMaybe[A,B](xs: Traversable[A])(f: A => B) = {
		xs.map(f(_)).filter(_ != null)
	}
	import collection.mutable.{LinkedHashMap, LinkedHashSet}

	implicit def traversableToGroupByOrderedImplicit[A](t: Traversable[A]) = new {
		def groupByOrdered[K](f: A => K): LinkedHashMap[K, LinkedHashSet[A]] = {
		    val map = LinkedHashMap[K,LinkedHashSet[A]]()
		    for (i <- t) {
		    	val key = f(i)
    			map(key) = map.lift(key).getOrElse(LinkedHashSet[A]()) + i
		    }
		    map
		}
	}
}

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
  def items(xs : java.lang.Iterable[Album], long : Boolean = true): java.util.List[Album] = {
    new java.util.ArrayList(items(xs.asScala, long).toList.asJava)
  }
  protected def items(xs : Traversable[Album], long : Boolean) = {
	xs
	.map(x => (recordings.replaceAllIn(x.getName(), m => m.group(1)?:"" + m.group(2)?:"" + " ..."), x))
	.groupByOrdered(x =>
	  (if (long) number_initial else numbers).replaceAllIn(punct.replaceAllIn(x._1, " ").toLowerCase(), x => {
	    var ns = for (
	      i <- (1 to x.groupCount) if x.group(i) != null;
	      n = if(i < 22) i - 1 else if (i < 29) 20 + (i - 21) * 20 else if (i < 41) i - 29 else 0/*x.group(i)*/
	    ) yield n
	    ns.sum.formatted("ZZ%04d")
	  }))
	 .valuesIterator.map(xs => {
	  val x = xs.head
	  val _xs = xs.tail
	  if (_xs.isEmpty)
	    x._2
	  else
		new AlbumGroup(x._1, xs.map(_._2).toList)
	})
  }
}