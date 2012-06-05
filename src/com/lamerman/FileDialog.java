package com.lamerman;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

public class FileDialog extends ListActivity {
    // @see https://code.google.com/p/android-file-dialog/issues/detail?id=3
    // @see http://twigstechtips.blogspot.com.au/2011/11/for-my-app-moustachify-everything-i-was.html
    // This is purely a data storage class for saving information between rotations
    private class LastConfiguration {
        public String m_strCurrentPath;

        public LastConfiguration(String currentPath) {
            this.m_strCurrentPath = currentPath;
        }
    }    
    
	private static final String ITEM_KEY = "key";
	private static final String ITEM_IMAGE = "image";
	private static final String ITEM_FILE = "file";
	
	
	public static final String PATH_ROOT = "/";
	public static final String PATH_SDCARD = Environment.getExternalStorageDirectory().getAbsolutePath();

	private FileDialogOptions options;
	
	// TODO: This needs a cleanup
	private List<String> path = null;
	private TextView myPath;
	private EditText mFileName;

	private Button selectButton;

	private LinearLayout layoutSelect;
	private LinearLayout layoutCreate;
	private InputMethodManager inputManager;
	private String parentPath;
	private String currentPath = PATH_ROOT;

	private File selectedFile;
	private HashMap<String, Integer> lastPositions = new HashMap<String, Integer>();

	/**
	 * Called when the activity is first created.
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setResult(RESULT_CANCELED, getIntent());

		setContentView(R.layout.file_dialog_main);
		myPath = (TextView) findViewById(R.id.path);
		mFileName = (EditText) findViewById(R.id.fdEditTextFile);

		// Read options
		options = new FileDialogOptions(getIntent());
		
		// Hide the titlebar if needed
		if (options.titlebarForCurrentPath) {
		    myPath.setVisibility(View.GONE);
		}

		inputManager = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

		selectButton = (Button) findViewById(R.id.fdButtonSelect);
		selectButton.setEnabled(options.chooseFolder);
		selectButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				if(options.chooseFolder){
					returnFilename(currentPath);
				}
				if (selectedFile != null) {
					returnFilename(selectedFile.getPath());
				}
			}
		});

		final Button newButton = (Button) findViewById(R.id.fdButtonNew);
		newButton.setOnClickListener(new OnClickListener() {
			public void onClick(View v) {
				setCreateVisible(v);

				mFileName.setText("");
				mFileName.requestFocus();
			}
		});

		if (!options.allowCreate) {
			newButton.setEnabled(false);
		}

		layoutSelect = (LinearLayout) findViewById(R.id.fdLinearLayoutSelect);
		layoutCreate = (LinearLayout) findViewById(R.id.fdLinearLayoutCreate);
		layoutCreate.setVisibility(View.GONE);


		// If the New button is disabled and it's one click select, hide the selection layout.
		if (!options.allowCreate && options.oneClickSelect) {
		    layoutSelect.setVisibility(View.GONE);
		}		

		final Button cancelButton = (Button) findViewById(R.id.fdButtonCancel);
		cancelButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				setSelectVisible(v);
			}

		});
		final Button createButton = (Button) findViewById(R.id.fdButtonCreate);
		createButton.setOnClickListener(new OnClickListener() {

			public void onClick(View v) {
				if (mFileName.getText().length() > 0) {
				    StringBuilder sb = new StringBuilder();
				    
				    sb.append(currentPath);
				    sb.append(File.separator);
				    sb.append(mFileName.getText());
				    
				    returnFilename(sb.toString());
				}
			}
		});

		// Try to restore current path after screen rotation
		LastConfiguration lastConfiguration = (LastConfiguration) getLastNonConfigurationInstance();

		if (lastConfiguration != null) {
		    getDir(lastConfiguration.m_strCurrentPath);
		}
		// New instance of FileDialog
		else {
		    File file = new File(options.currentPath);
		    
		    if (file.isDirectory() && file.exists()) {
		        getDir(options.currentPath);
		    }
		    else {
		        getDir(PATH_ROOT);
		    }
		}
	}

	private void getDir(String dirPath) {

		boolean useAutoSelection = dirPath.length() < currentPath.length();

		Integer position = lastPositions.get(parentPath);

		getDirImpl(dirPath);

		if (position != null && useAutoSelection) {
			getListView().setSelection(position);
		}

	}

	private void getDirImpl(final String dirPath) {
		currentPath = dirPath;

		path = new ArrayList<String>();
		ArrayList<HashMap<String, Object>> mList = new ArrayList<HashMap<String, Object>>();

		File f = new File(currentPath);
		File[] files = f.listFiles();
		
		// Null if file is not a directory
		if (files == null) {
			currentPath = PATH_ROOT;
			f = new File(currentPath);
			files = f.listFiles();
		}
		
		// Sort files by alphabet and ignore casing
		Arrays.sort(files);

		if (options.titlebarForCurrentPath) {
		    this.setTitle(currentPath);
		}
		else {
		    myPath.setText(getText(R.string.location) + ": " + currentPath);
		}

		/*
         * http://stackoverflow.com/questions/5090915/show-songs-from-sdcard
         * http://developer.android.com/reference/android/os/Environment.html
         * http://stackoverflow.com/questions/5453708/android-how-to-use-environment-getexternalstoragedirectory
         */
        if (currentPath.equals(PATH_ROOT)) {
            boolean mounted = Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
            
            if (mounted) {
                addItem(mList, PATH_SDCARD + "(SD Card)", this.options.iconSDCard,null);
                path.add(PATH_SDCARD);
            }
        }
		
		if (!currentPath.equals(PATH_ROOT)) {
			addItem(mList, "/ (Root folder)", this.options.iconUp,null);
			path.add(PATH_ROOT);

			addItem(mList, "../ (Parent folder)", this.options.iconUp,null);
			path.add(f.getParent());
			parentPath = f.getParent();
		}
		
		TreeMap<String, String> dirsMap = new TreeMap<String, String>();
		TreeMap<String, String> dirsPathMap = new TreeMap<String, String>();
		TreeMap<String, String> filesMap = new TreeMap<String, String>();
		TreeMap<String, String> filesPathMap = new TreeMap<String, String>();

		for (File file : files) {
			if (file.isDirectory()) {
				String dirName = file.getName();
				dirsMap.put(dirName, dirName);
				dirsPathMap.put(dirName, file.getPath());
			} else {
				filesMap.put(file.getName(), file.getName());
				filesPathMap.put(file.getName(), file.getPath());
			}
		}
		
		path.addAll(dirsPathMap.tailMap("").values());
		path.addAll(filesPathMap.tailMap("").values());

		for (String dir : dirsMap.tailMap("").values()) {
			addItem(mList, dir, this.options.iconFolder,(String)dirsPathMap.get(dir));
		}

		for (String file : filesMap.tailMap("").values()) {
			addItem(mList, file, this.options.iconFile,(String)filesPathMap.get(file));
		}
		final Boolean chooseFolder = options.chooseFolder;
		SimpleAdapter fileList = new SimpleAdapter(this, mList,
            R.layout.file_dialog_row,
            new String[] { ITEM_KEY, ITEM_IMAGE },
            new int[] { R.id.fdrowtext, R.id.fdrowimage }
        ){
			@Override
			public View getView(int position, View convertView, ViewGroup parent){				
				View view = super.getView(position, convertView, parent);
				Log.w("SimpleAdapter", "getting "+position);
				Button button = (Button)view.findViewById(R.id.choose);
				
				Object object = this.getItem(position);
				Log.w("SimpleAdapter", object==null?"null":object.toString());
				HashMap<?,?> item = (HashMap<?,?>)object;
				object = item.get(ITEM_FILE);
				String file_path = object==null?null:object.toString();
				Log.w("SimpleAdapter", file_path==null?"null":file_path);
				if(file_path == null){
					button.setVisibility(View.GONE);
					return view;
				}
				final File file = new File(file_path);
				if(chooseFolder && !file.isDirectory()){
					button.setVisibility(View.GONE);
					return view;
				}
				if(!chooseFolder && file.isDirectory()){
					button.setVisibility(View.GONE);
					return view;
				}
				
				button.setOnClickListener(new OnClickListener() {
					public void onClick(View v) {
						if (file != null) {
						    returnFilename(file.getPath());
						}
					}
				});
				
				button.setVisibility(View.VISIBLE);
				return view;
			}
		};
      
		fileList.notifyDataSetChanged();

		setListAdapter(fileList);
	}

	private void addItem(ArrayList<HashMap<String, Object>> mList, String fileName, int imageId,String path) {
		HashMap<String, Object> item = new HashMap<String, Object>();
		item.put(ITEM_KEY, fileName);
		item.put(ITEM_IMAGE, imageId);
		item.put(ITEM_FILE, path);
		mList.add(item);
	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		File file = new File(path.get(position));

		setSelectVisible(v);

		if (!file.exists()) {
		    new AlertDialog.Builder(this)
                .setIcon(R.drawable.icon)
                .setTitle("Does not exist.")
                .setMessage(file.getName())
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                    }
                })
                .show();
		    return;
		}
		
		if (file.isDirectory()) {
			selectButton.setEnabled(options.chooseFolder);
			if (file.canRead()) {
			    // Save the scroll position so users don't get confused when they come back
				lastPositions.put(currentPath, this.getListView().getFirstVisiblePosition());
				getDir(path.get(position));
			} else {
				new AlertDialog.Builder(this)
						.setIcon(R.drawable.icon)
						.setTitle(
								"[" + file.getName() + "] "
										+ getText(R.string.cant_read_folder))
						.setPositiveButton("OK",
								new DialogInterface.OnClickListener() {

									public void onClick(DialogInterface dialog,
											int which) {

									}
								}).show();
			}
		}
		else {
			selectedFile = file;
			v.setSelected(true);
			selectButton.setEnabled(true);
			
			if (options.oneClickSelect) {
			    selectButton.performClick();
			}
		}
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK)) {
			selectButton.setEnabled(false);

			if (layoutCreate.getVisibility() == View.VISIBLE) {
				layoutCreate.setVisibility(View.GONE);
				layoutSelect.setVisibility(View.VISIBLE);
			} else {
				if (!currentPath.equals(PATH_ROOT)) {
					getDir(parentPath);
				} else {
					return super.onKeyDown(keyCode, event);
				}
			}

			return true;
		} else {
			return super.onKeyDown(keyCode, event);
		}
	}

	private void setCreateVisible(View v) {
		layoutCreate.setVisibility(View.VISIBLE);
		layoutSelect.setVisibility(View.GONE);

		inputManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
		selectButton.setEnabled(false);
	}

	private void setSelectVisible(View v) {
	    if (options.oneClickSelect) {
	        return;
	    }
	    
		layoutCreate.setVisibility(View.GONE);
		layoutSelect.setVisibility(View.VISIBLE);

		inputManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
		selectButton.setEnabled(false);
	}
	
	
	private void returnFilename(String filepath) {
	    this.options.currentPath = currentPath;
	    this.options.selectedFile = filepath;

	    setResult(RESULT_OK, options.createResultIntent());
	    finish();
	}
	
	// Remember the information when the screen is just about to be rotated.
	// This information can be retrieved by using getLastNonConfigurationInstance()
	public Object onRetainNonConfigurationInstance() {
	    return new LastConfiguration(this.currentPath);
	}
}