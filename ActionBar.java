package com.infoconcert.infoconcert.views;

import android.app.Activity;
import android.content.Context;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.infoconcert.infoconcert.R;

/**
 * Cette classe est une vue personnalisée qui permet de gérer une actionbar
 * style Honeycomb sur les anciens appareils.
 * Ne pas oublier d'appliquer le style ActionBar quand elle est inclue !
 */
public class ActionBar extends RelativeLayout implements OnClickListener {
	private LayoutInflater inflater;
	private View actionBarView;
	private View logoView;
	private View searchView;
	private View shareView;
	private View titleView;
	private View mapView;
	private View gpsView;
	private OnClickListener searchClickListener;
	private OnClickListener shareClickListener;
	private OnClickListener mapClickListener;
	private OnClickListener gpsClickListener;
	private boolean enableLogoBackAction;
	private Context context;
	
	public ActionBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        
        this.context = context;
        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        actionBarView = inflater.inflate(R.layout.actionbar, null);
        addView(actionBarView);
        
        enableLogoBackAction = false;
        logoView = actionBarView.findViewById(R.id.actionbar_logo);
        logoView.setOnClickListener(this);
        
        titleView = actionBarView.findViewById(R.id.actionbar_title);
        titleView.setOnClickListener(this);
        titleView.setVisibility(View.GONE);
        
        searchView = actionBarView.findViewById(R.id.actionbar_search);
        searchView.setOnClickListener(this);
        searchView.setVisibility(View.GONE);
        
        shareView = actionBarView.findViewById(R.id.actionbar_share);
        shareView.setOnClickListener(this);
        shareView.setVisibility(View.GONE);
        
        mapView = actionBarView.findViewById(R.id.actionbar_map);
        mapView.setOnClickListener(this);
        mapView.setVisibility(View.GONE);
        
        gpsView = actionBarView.findViewById(R.id.actionbar_gps);
        gpsView.setOnClickListener(this);
        gpsView.setVisibility(View.GONE);
	}

	/**
	 * Affiche le bouton recherche et lui affecte une action
	 * @param listener : La classe qui va répondre à l'evenement click du bouton. Si cette valeur est nulle, l'action est inchangée
	 */
	public void setSearchButtonParams(boolean show, OnClickListener listener) {
		if(listener != null) {
			searchClickListener = listener;
		}
		searchView.setVisibility(show ? View.VISIBLE : View.GONE);
	}
	
	/**
	 * Affiche le bouton partager et lui affecte une action
	 * @param listener : La classe qui va répondre à l'evenement click du bouton. Si cette valeur est nulle, l'action est inchangée
	 */
	public void setShareButtonParams(boolean show, OnClickListener listener) {
		if(listener != null) {
			shareClickListener = listener;
		}
		shareView.setVisibility(show ? View.VISIBLE : View.GONE);
	}
	
	/**
	 * Affiche le bouton carte et lui affecte une action
	 * @param listener : La classe qui va répondre à l'evenement click du bouton. Si cette valeur est nulle, l'action est inchangée
	 */
	public void setMapButtonParams(boolean show, OnClickListener listener) {
		if(listener != null) {
			mapClickListener = listener;
		}
		mapView.setVisibility(show ? View.VISIBLE : View.GONE);
	}
	
	/**
	 * Affiche le bouton gps et lui affecte une action
	 * @param listener : La classe qui va répondre à l'evenement click du bouton. Si cette valeur est nulle, l'action est inchangée
	 */
	public void setGpsButtonParams(boolean show, OnClickListener listener) {
		if(listener != null) {
			gpsClickListener = listener;
		}
		gpsView.setVisibility(show ? View.VISIBLE : View.GONE);
	}
	
	/**
	 * Active ou non l'action retour du logo/titre (si possible).
	 * @param enable
	 */
	public void setLogoBackActionEnabled(boolean enable) {
		this.enableLogoBackAction = enable;
	}
	
	/**
	 * Cache le logo et affiche un titre
	 * @param title Le titre à afficher
	 */
	public void setTitle(String title) {
		((TextView) titleView).setText(title);
		logoView.setVisibility(View.GONE);
		titleView.setVisibility(View.VISIBLE);
	}
	
	/**
	 * Cache le logo et affiche un titre
	 * @param title Le titre à afficher
	 */
	public void setTitle(int res) {
		setTitle(getResources().getString(res));
	}
	
	@Override
	public void onClick(View v) {
		if(v == searchView) {
			if(searchClickListener != null) {
				searchClickListener.onClick(v);
			}
		} else if(v == logoView || v == titleView) {
			if(enableLogoBackAction && context instanceof Activity) {
				((Activity) context).finish();
			}
		} else if(v == shareView) {
			if(shareClickListener != null) {
				shareClickListener.onClick(v);
			}
		} else if(v == mapView) {
			if(mapClickListener != null) {
				mapClickListener.onClick(v);
			}
		} else if(v == mapView) {
			if(gpsClickListener != null) {
				gpsClickListener.onClick(v);
			}
		}
	}
}
