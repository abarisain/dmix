package net.roarsoftware.lastfm;

import java.util.Collection;

/**
 * A <code>PaginatedResult</code> is returned by methods which result set might be so large that it needs
 * to be paginated. Each <code>PaginatedResult</code> contains the total number of result pages, the current
 * page and a <code>Collection</code> of entries for the current page.
 *
 * @author Janni Kovacs
 */
public class PaginatedResult<T> {

	private int page;
	private int totalPages;
	private Collection<T> pageResults;

	PaginatedResult(int page, int totalPages, Collection<T> pageResults) {
		this.page = page;
		this.totalPages = totalPages;
		this.pageResults = pageResults;
	}

	/**
	 * Returns the page number of this result.
	 *
	 * @return page number
	 */
	public int getPage() {
		return page;
	}

	/**
	 * Returns a list of entries of the type <code>T</code> for this page.
	 *
	 * @return page results
	 */
	public Collection<T> getPageResults() {
		return pageResults;
	}

	/**
	 * Returns the total number of pages available.
	 *
	 * @return total pages
	 */
	public int getTotalPages() {
		return totalPages;
	}
}
