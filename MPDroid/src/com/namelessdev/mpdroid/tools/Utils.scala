package com.namelessdev.mpdroid.tools

import collection.JavaConverters._
import org.a0z.mpd._
import org.a0z.mpd.exception.MPDServerException
import java.util.ConcurrentModificationException

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
		def groupByOrdered[K](f: A => K)(implicit ord: Ordering[K]): collection.SortedMap[K, List[A]] = {
		    var map = collection.SortedMap[K,List[A]]()
		    for (i <- t) {
		    	val key = f(i)
    			map = map + ((key, i :: map.lift(key).getOrElse(List[A]())))
		    }
		    map
		}

		def groupByStable[K](f: A => K): LinkedHashMap[K, List[A]] = {
		    val map = LinkedHashMap[K,List[A]]()
		    for (i <- t) {
		    	val key = f(i)
    			map(key) = i :: map.lift(key).getOrElse(List[A]())
		    }
		    map
		}
	}
	
	def retry(f: Function.MPDAction0): Unit = {
	  var retry = 0
	  while(retry <= 3)
		try {
		  f();
		  return;
		} catch {
		  case e: MPDServerException => {
		    Log.w(e);
		    retry += 1;
		    try {
			  Thread.sleep(1000^retry);
			} catch {
			  case e : Throwable => {}
			}
		  }
		  case e: ConcurrentModificationException => {
			Log.w(e);
		  }
		  case e: Throwable => {
		    Log.e(e);
		    return;
		  }
		}
	}
}