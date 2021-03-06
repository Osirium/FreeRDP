/*
   Main/Home Activity

   Copyright 2013 Thinstuff Technologies GmbH, Author: Martin Fleisz

   This Source Code Form is subject to the terms of the Mozilla Public License, v. 2.0. 
   If a copy of the MPL was not distributed with this file, You can obtain one at http://mozilla.org/MPL/2.0/.
*/

package com.freerdp.afreerdp.presentation;

import java.util.ArrayList;

import com.freerdp.afreerdp.R;
import com.freerdp.afreerdp.application.GlobalApp;
import com.freerdp.afreerdp.application.GlobalSettings;
import com.freerdp.afreerdp.domain.BookmarkBase;
import com.freerdp.afreerdp.domain.ConnectionReference;
import com.freerdp.afreerdp.domain.PlaceholderBookmark;
import com.freerdp.afreerdp.domain.QuickConnectBookmark;
import com.freerdp.afreerdp.utils.BookmarkArrayAdapter;
import com.freerdp.afreerdp.utils.SeparatedListAdapter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu;
import android.view.View.OnClickListener;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnCreateContextMenuListener;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.AdapterView;
import android.widget.AdapterView.AdapterContextMenuInfo;

public class HomeActivity extends Activity
{	
	private final static String ADD_BOOKMARK_PLACEHOLDER = "add_bookmark";
	
	private ListView listViewBookmarks;
	private WebView webViewGetStarted;

	private Button clearTextButton;
	private EditText superBarEditText;
	private BookmarkArrayAdapter manualBookmarkAdapter;
	private SeparatedListAdapter separatedListAdapter;
	
	private PlaceholderBookmark addBookmarkPlaceholder; 
	
	private static final String TAG = "HomeActivity";
	
	private static final String PARAM_SUPERBAR_TEXT = "superbar_text";

	private String sectionLabelBookmarks;


	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		setTitle(R.string.title_home);		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.home);
		
		long heapSize = Runtime.getRuntime().maxMemory();
		Log.i(TAG, "Max HeapSize: " + heapSize);
		Log.i(TAG, "App data folder: " + getFilesDir().toString());		
			
		// load strings
		sectionLabelBookmarks = getResources().getString(R.string.section_bookmarks); 
		
		// create add bookmark/quick connect bookmark placeholder
		addBookmarkPlaceholder = new PlaceholderBookmark();
		addBookmarkPlaceholder.setName(ADD_BOOKMARK_PLACEHOLDER);
		addBookmarkPlaceholder.setLabel(getResources().getString(R.string.list_placeholder_add_bookmark));
		
		// load views
		clearTextButton = (Button) findViewById(R.id.clear_search_btn);
		superBarEditText = (EditText) findViewById(R.id.superBarEditText);		
		
		listViewBookmarks = (ListView) findViewById(R.id.listViewBookmarks);		
		webViewGetStarted = (WebView) findViewById(R.id.webViewWelcome);
		
		String filename = ((getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_LARGE) ? "welcome.html" : "welcome_phone.html";
		webViewGetStarted.loadUrl("file:///android_asset/welcome_page/" + filename);		
		
		// set listeners for the list view 
		listViewBookmarks.setOnItemClickListener(new AdapterView.OnItemClickListener() {
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				String curSection = separatedListAdapter.getSectionForPosition(position);
				Log.v(TAG, "Clicked on item id " + separatedListAdapter.getItemId(position) + " in section " + curSection);
				if(curSection == sectionLabelBookmarks)
				{	
					String refStr = view.getTag().toString();									
					if (ConnectionReference.isManualBookmarkReference(refStr) ||
						ConnectionReference.isHostnameReference(refStr))
					{				
						Bundle bundle = new Bundle();
						bundle.putString(SessionActivity.PARAM_CONNECTION_REFERENCE, refStr);				

						Intent sessionIntent = new Intent(view.getContext(), SessionActivity.class);				
						sessionIntent.putExtras(bundle);						
						startActivity(sessionIntent);						
						
						// clear any search text
						superBarEditText.setText("");
						superBarEditText.clearFocus();
					}
					else if (ConnectionReference.isPlaceholderReference(refStr))
					{
						// is this the add bookmark placeholder?
						if (ConnectionReference.getPlaceholder(refStr).equals(ADD_BOOKMARK_PLACEHOLDER))
						{
							Intent bookmarkIntent = new Intent(view.getContext(), BookmarkActivity.class);				
							startActivity(bookmarkIntent);																		
						}
					}
				}
			}
		});
				
		listViewBookmarks.setOnCreateContextMenuListener(new OnCreateContextMenuListener() {	
			@Override
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
				// if the selected item is not a session item (tag == null) and not a quick connect entry 
				// (not a hostname connection reference) inflate the context menu
				View itemView = ((AdapterContextMenuInfo)menuInfo).targetView;
				String refStr = itemView.getTag() != null ? itemView.getTag().toString() : null; 
				if (refStr != null && !ConnectionReference.isHostnameReference(refStr) && !ConnectionReference.isPlaceholderReference(refStr))
				{
					getMenuInflater().inflate(R.menu.bookmark_context_menu, menu);
					menu.setHeaderTitle(getResources().getString(R.string.menu_title_bookmark));					
				}
            }
		});
		
		superBarEditText.addTextChangedListener(new SuperBarTextWatcher());		
		
		clearTextButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				superBarEditText.setText("");				
			}			
		});
		
		webViewGetStarted.setWebViewClient(new WebViewClient() {
			@Override
			public boolean shouldOverrideUrlLoading(WebView view, String url)
			{
				if (url.endsWith("new_connection"))
				{
					Intent bookmarkIntent = new Intent(getApplicationContext(), BookmarkActivity.class);
					startActivity(bookmarkIntent);				
					return true;
				}
				
				return false;
			}
		});			
	}
	
	@Override
	public boolean onSearchRequested() {
		superBarEditText.requestFocus();
		return true;
	}
	
	@Override
	public boolean onContextItemSelected(MenuItem aItem) {

		// get connection reference
		AdapterContextMenuInfo menuInfo = (AdapterContextMenuInfo)aItem.getMenuInfo();			
		String refStr = menuInfo.targetView.getTag().toString();			
		
		switch(aItem.getItemId()) {
		
			case R.id.bookmark_connect:
			{
				Bundle bundle = new Bundle();
				bundle.putString(SessionActivity.PARAM_CONNECTION_REFERENCE, refStr);				
				Intent sessionIntent = new Intent(this, SessionActivity.class);				
				sessionIntent.putExtras(bundle);				
	
				startActivity(sessionIntent);					
				return true;
			}
			
			case R.id.bookmark_edit:
			{
				Bundle bundle = new Bundle();
				bundle.putString(BookmarkActivity.PARAM_CONNECTION_REFERENCE, refStr);

				Intent bookmarkIntent = new Intent(this.getApplicationContext(), BookmarkActivity.class);
				bookmarkIntent.putExtras(bundle);				
				startActivity(bookmarkIntent);
				return true;
			}
		
			case R.id.bookmark_delete:
			{
				if(ConnectionReference.isManualBookmarkReference(refStr))
				{
					long id = ConnectionReference.getManualBookmarkId(refStr);
					GlobalApp.getManualBookmarkGateway().delete(id);
					manualBookmarkAdapter.remove(id);
					separatedListAdapter.notifyDataSetChanged();
				}
				else
				{
					assert false;
				}
				
				showWelcomeScreenOrBookmarkList();
				
				// clear super bar text
				superBarEditText.setText("");
				return true;				
			}					
		}
		
		return false;
	}	
	
	@Override
	protected void onResume() {
		super.onResume();
		Log.v(TAG, "HomeActivity.onResume");
		
		// create bookmark cursor adapter
		manualBookmarkAdapter = new BookmarkArrayAdapter(this, R.layout.bookmark_list_item, GlobalApp.getManualBookmarkGateway().findAll());
		
		// add add bookmark item to manual adapter
		manualBookmarkAdapter.insert(addBookmarkPlaceholder, 0);
		
		// attach all adapters to the separatedListView adapter and assign it to the list view
		separatedListAdapter = new SeparatedListAdapter(this);
		separatedListAdapter.addSection(sectionLabelBookmarks, manualBookmarkAdapter);
		listViewBookmarks.setAdapter(separatedListAdapter);		
		
		// show welcome screen in case we have a first-time user
		showWelcomeScreenOrBookmarkList();
		
		// if we have a filter text entered cause an update to be caused here
		String filter = superBarEditText.getText().toString();
		if (filter.length() > 0)
			superBarEditText.setText(filter);
	}

	@Override
	protected void onPause() {
		super.onPause();
		Log.v(TAG, "HomeActivity.onPause");

		// reset adapters
		listViewBookmarks.setAdapter(null);
		separatedListAdapter = null;
		manualBookmarkAdapter = null;
	}
	
	@Override
	public void onBackPressed()
	{
		// if back was pressed - ask the user if he really wants to exit 
		if (GlobalSettings.getAskOnExit())
		{
			final CheckBox cb = new CheckBox(this);
			cb.setTextAppearance(this, android.R.style.TextAppearance_Medium_Inverse);
			cb.setChecked(!GlobalSettings.getAskOnExit());
			cb.setText(R.string.dlg_dont_show_again);
			
			AlertDialog.Builder builder = new AlertDialog.Builder(this);
			builder.setTitle(R.string.dlg_title_exit)
			.setMessage(R.string.dlg_msg_exit)
			.setView(cb)
			.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int which) 
			    {
			    	GlobalSettings.setAskOnExit(!cb.isChecked());
			    	finish();
			    }
			})
			.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
			    public void onClick(DialogInterface dialog, int which) 
			    {
			    	GlobalSettings.setAskOnExit(!cb.isChecked());		    	
			    	dialog.dismiss();
			    }
			})
			.create()
			.show();			
		}
		else
		{
			super.onBackPressed();
		}
	}
	
	@Override
	protected void onSaveInstanceState(Bundle outState)
	{
		super.onSaveInstanceState(outState);
		outState.putString(PARAM_SUPERBAR_TEXT, superBarEditText.getText().toString());
	}
	
	@Override
	protected void onRestoreInstanceState(Bundle inState)
	{
		super.onRestoreInstanceState(inState);
		superBarEditText.setText(inState.getString(PARAM_SUPERBAR_TEXT));
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.home_menu, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {

		switch (item.getItemId()) {
		
			case R.id.newBookmark:
			{
				Intent bookmarkIntent = new Intent(this, BookmarkActivity.class);
				startActivity(bookmarkIntent);				
				break;
			}
			
			case R.id.appSettings:
			{
				Intent settingsIntent = new Intent(this, ApplicationSettingsActivity.class);
				startActivity(settingsIntent);
				break;
			}
			
			case R.id.help:
			{
				Intent helpIntent = new Intent(this, HelpActivity.class);
				startActivity(helpIntent);
				break;
			}

			case R.id.exit:
			{
				finish();
				break;
			}
				
			case R.id.about:
			{
				Intent aboutIntent = new Intent(this, AboutActivity.class);
				startActivity(aboutIntent);
				break;
			}
		}

		return true;
	}	
	
	private void showWelcomeScreenOrBookmarkList()
	{
		listViewBookmarks.setVisibility(View.VISIBLE);
		webViewGetStarted.setVisibility(View.GONE);
	}
	
	private class SuperBarTextWatcher implements TextWatcher
	{
		@Override
		public void afterTextChanged(Editable s) {
			if(separatedListAdapter != null)
			{
				String text = s.toString();
				if(text.length() > 0)
				{									
					ArrayList<BookmarkBase> computers_list = GlobalApp.getQuickConnectHistoryGateway().findHistory(text);
					computers_list.addAll(GlobalApp.getManualBookmarkGateway().findByLabelOrHostnameLike(text));
					manualBookmarkAdapter.replaceItems(computers_list);
					QuickConnectBookmark qcBm = new QuickConnectBookmark();
					qcBm.setLabel(text);
					qcBm.setHostname(text);
					manualBookmarkAdapter.insert(qcBm, 0);
				}
				else
				{
					manualBookmarkAdapter.replaceItems(GlobalApp.getManualBookmarkGateway().findAll());
					manualBookmarkAdapter.insert(addBookmarkPlaceholder, 0);
				}
				
				separatedListAdapter.notifyDataSetChanged();
			}
		}

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count, int after) {
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before, int count) {
		}		
	}
}
