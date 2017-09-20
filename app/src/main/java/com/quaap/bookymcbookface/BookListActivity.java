package com.quaap.bookymcbookface;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.text.util.Linkify;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.quaap.bookymcbookface.book.Book;
import com.quaap.bookymcbookface.book.BookMetadata;

/**
 * Copyright (C) 2017   Tom Kliethermes
 *
 * This file is part of BookyMcBookface and is is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as published by the
 * Free Software Foundation; either version 3 of the License, or (at your option) any
 * later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */

public class BookListActivity extends Activity {

    private SharedPreferences data;

    private int nextid = 0;

    private ViewGroup listHolder;
    private ScrollView listScroller;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_list);

        listHolder = (ViewGroup)findViewById(R.id.book_list_holder);
        listScroller = (ScrollView)findViewById(R.id.book_list_scroller);

//        findViewById(R.id.add_button).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                findFile();
//            }
//        });
//        findViewById(R.id.add_dir_button).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                findDir();
//            }
//        });
//
//        findViewById(R.id.about_button).setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View v) {
//                showMsg(BookListActivity.this,getString(R.string.about), getString(R.string.about_app));
//            }
//        });
        checkStorageAccess(false);

        data = getSharedPreferences("booklist", Context.MODE_PRIVATE);

    }

    @Override
    protected void onResume() {
        super.onResume();
        populateBooks();

    }

    private enum SortOrder {Default, Title, Author}

    private void setSortOrder(SortOrder sortOrder) {
        data.edit().putString("sortorder",sortOrder.name()).apply();
    }

    private void populateBooks() {

        SortOrder sortorder = SortOrder.valueOf(data.getString("sortorder", SortOrder.Default.name()));
        nextid = data.getInt("nextid",0);

        final int [] order = new int[nextid];

        for (int i = 0; i < nextid; i++) {
            order[i] = -1;
        }
        if (sortorder==SortOrder.Default) {
            for (int i = 0; i < nextid; i++) {
                order[i] = i;
            }

        } else {
            Set<String> list;
            if (sortorder==SortOrder.Title) {
                list = data.getStringSet("title_order", null);
            } else {
                list = data.getStringSet("author_order", null);
            }
            if (list!=null) {
                int i = 0;
                List<String> alist = new ArrayList<>(list);
                Collections.sort(alist);
                for(String p: alist) {
                    Matcher m = Pattern.compile("\\.(\\d+)$").matcher(p);
                    if (m.find()) {
                        order[i++] = Integer.parseInt(m.group(1));
                    }

                }
            }
        }


        listHolder.removeAllViews();
        new AsyncTask<Void,Void,Void>() {

            @Override
            protected Void doInBackground(Void... voids) {
                for (int i=0; i<order.length; i++) {
                    final int id = order[i];
                    listScroller.post(new Runnable() {
                        @Override
                        public void run() {

                            displayBookListEntry(id);
                        }
                    });
                }
                return null;
            }
        }.execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        switch (item.getItemId()) {
            case R.id.menu_add:
                findFile();
                return true;
            case R.id.menu_add_dir:
                findDir();
                return true;
            case R.id.menu_about:
                showMsg(BookListActivity.this,getString(R.string.about), getString(R.string.about_app));
                return true;
            case R.id.menu_sort_default:
                setSortOrder(SortOrder.Default);
                populateBooks();
                return true;
            case R.id.menu_sort_author:
                setSortOrder(SortOrder.Author);
                populateBooks();
                return true;
            case R.id.menu_sort_title:
                setSortOrder(SortOrder.Title);
                populateBooks();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }

    private void displayBookListEntry(int bookid) {
        String bookidstr = "book." + bookid;
        String filename = data.getString(bookidstr + ".filename", null);


        if (filename!=null) {
            Log.d("Book", "Filename "  + filename);
            String title = data.getString(bookidstr + ".title", null);
            String author = data.getString(bookidstr + ".author", null);
            ViewGroup listEntry = (ViewGroup)getLayoutInflater().inflate(R.layout.book_list_item, listHolder, false);
            TextView titleView = (TextView)listEntry.findViewById(R.id.book_title);
            TextView authorView = (TextView)listEntry.findViewById(R.id.book_author);
            TextView statusView = (TextView)listEntry.findViewById(R.id.book_status);

            titleView.setText(title);
            authorView.setText(author);
            long lastread = data.getLong(bookidstr + ".lastread", Long.MIN_VALUE);

            if (lastread!=Long.MIN_VALUE) {

                statusView.setText(getString(R.string.book_viewed_on, android.text.format.DateUtils.getRelativeTimeSpanString(lastread)));
                //statusView.setText(getString(R.string.book_viewed_on, new SimpleDateFormat("YYYY-MM-dd HH:mm", Locale.getDefault()).format(new Date(lastread))));
            }
            listEntry.setTag(bookidstr);
            listEntry.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    readBook((String)view.getTag());
                }
            });

            listEntry.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View view) {
                    longClickBook(view);
                    return false;
                }
            });

            if (data.getString("lastread", "").equals(bookidstr)) {
                listHolder.addView(listEntry,0);
            } else {
                listHolder.addView(listEntry);
            }
        }
    }

    private void readBook(String bookid) {
        String filename = data.getString(bookid + ".filename",null);
        if (filename!=null) {
            data.edit().putLong(bookid + ".lastread", System.currentTimeMillis()).putString("lastread", bookid).apply();

            Intent main = new Intent(BookListActivity.this, ReaderActivity.class);
            main.putExtra("filename", filename);
            startActivity(main);
        }
    }

    private void removeBook(String bookid) {
        String file = data.getString(bookid + ".filename", null);
        String title = data.getString(bookid + ".title", null);
        String author = data.getString(bookid + ".author", null);
        int id = data.getInt(bookid + ".int", -1);

        if (file!=null) {
            Book.remove(this, new File(file));
        }

        Set<String> titles = new TreeSet<>(data.getStringSet("title_order", new TreeSet<String>()));
        titles.remove(title + "." + id );

        Set<String> authors = new TreeSet<>(data.getStringSet("author_order", new TreeSet<String>()));
        authors.remove(author + "." + id);


        data.edit()
                .remove(bookid + ".id")
                .remove(bookid + ".title")
                .remove(bookid + ".author")
                .remove(bookid + ".filename")
                .putStringSet("title_order",titles)
                .putStringSet("author_order",authors)
         .apply();
    }

    private boolean addBook(String filename) {
        return addBook(filename, true);
    }

    private boolean addBook(String filename, boolean showAlreadyAddedWarning) {
        if (data.getAll().values().contains(filename)) {

            if (showAlreadyAddedWarning) {
                Toast.makeText(this, getString(R.string.already_added, new File(filename).getName()), Toast.LENGTH_SHORT).show();
            }
            return true;
        }

        try {

            BookMetadata metadata = Book.getBookMetaData(this, filename);

            if (metadata!=null) {
                String bookid = "book." + nextid;

                String title = metadata.getTitle() != null ? metadata.getTitle().toLowerCase():"_" ;
                Set<String> titles = new TreeSet<>(data.getStringSet("title_order", new TreeSet<String>()));
                titles.add(title + "." + nextid );

                String author = metadata.getAuthor() != null ? metadata.getAuthor().toLowerCase():"_" ;

                String [] authparts = author.split("\\s+");
                if (authparts.length>1) {
                    author = authparts[authparts.length-1];
                    for (int i=0; i<authparts.length-1; i++) {
                        author += " " + authparts[i];
                    }
                }

                Set<String> authors = new TreeSet<>(data.getStringSet("author_order", new TreeSet<String>()));
                authors.add(author + "." + nextid);

                data.edit()
                        .putInt(bookid + ".id", nextid)
                        .putString(bookid + ".title", metadata.getTitle())
                        .putString(bookid + ".author", metadata.getAuthor())
                        .putString(bookid + ".filename", metadata.getFilename())
                        .putStringSet("title_order",titles)
                        .putStringSet("author_order",authors)
                .apply();

                displayBookListEntry(nextid);
                nextid++;
                data.edit().putInt("nextid",nextid).apply();
                return true;
            } else {
                Toast.makeText(this,getString(R.string.coulndt_add_book, new File(filename).getName()),Toast.LENGTH_SHORT).show();
            }

        } catch (IOException e) {
            Log.e("BookList", e.getMessage(), e);
        }
        return false;
    }

    private void findFile() {

        FsTools fsTools = new FsTools(this);

        if (checkStorageAccess(false)) {
            fsTools.selectExternalLocation(new FsTools.SelectionMadeListener() {
                @Override
                public void selected(File selection) {
                    addBook(selection.getPath());
                    populateBooks();

                }
            }, getString(R.string.find_book), false, Book.getFileExtensionRX());
        }
    }

    private void addDir(final File dir) {
        new AsyncTask<Void,Void,Void>() {
            @Override
            protected Void doInBackground(Void... voids) {

                for(final File file:dir.listFiles()) {
                    if (file.isFile() && file.getName().matches(Book.getFileExtensionRX())) {
                        listScroller.post(new Runnable() {
                            @Override
                            public void run() {
                                addBook(file.getPath(), false);
                            }
                        });
                    }
                }
                return null;
            }
        }.execute();
    }

    private void findDir() {

        FsTools fsTools = new FsTools(this);

        if (checkStorageAccess(false)) {
            fsTools.selectExternalLocation(new FsTools.SelectionMadeListener() {
                @Override
                public void selected(File selection) {
                    addDir(selection);
                    populateBooks();

                }
            }, getString(R.string.find_folder), true);
        }
    }


    private void longClickBook(final View view) {
        final String bookid = (String)view.getTag();
        PopupMenu menu = new PopupMenu(this, view);
        menu.getMenu().add(R.string.open_book).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                readBook(bookid);
                return false;
            }
        });
        menu.getMenu().add(R.string.remove_book).setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                removeBook(bookid);
                ((ViewGroup)view.getParent()).removeView(view);
                return false;
            }
        });
        menu.show();
    }

    private boolean checkStorageAccess(boolean yay) {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                    yay? REQUEST_READ_EXTERNAL_STORAGE : REQUEST_READ_EXTERNAL_STORAGE_NOYAY);
            return false;
        }
        return true;
    }

    private static final int REQUEST_READ_EXTERNAL_STORAGE_NOYAY = 4333;
    private static final int REQUEST_READ_EXTERNAL_STORAGE = 4334;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        boolean yay = true;
        switch (requestCode) {
            case REQUEST_READ_EXTERNAL_STORAGE_NOYAY:
                yay = false;
            case REQUEST_READ_EXTERNAL_STORAGE:
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    if (yay) Toast.makeText(this, "Yay", Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Boo", Toast.LENGTH_LONG).show();
                }

        }
    }

    public static void showMsg(Context context, String title, String message) {
        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(title);

        final TextView messageview = new TextView(context);
        messageview.setPadding(16,8,16,8);

        final SpannableString s = new SpannableString(message);
        Linkify.addLinks(s, Linkify.ALL);
        messageview.setText(s);
        messageview.setMovementMethod(LinkMovementMethod.getInstance());

        builder.setView(messageview);

        builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }
}