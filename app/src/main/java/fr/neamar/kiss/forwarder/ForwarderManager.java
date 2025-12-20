package fr.neamar.kiss.forwarder;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;

import fr.neamar.kiss.MainActivity;
import fr.neamar.kiss.R;
import fr.neamar.kiss.SettingsActivity;
import fr.neamar.kiss.ui.ListPopup;

public class ForwarderManager extends Forwarder {
    private final Widgets widgetsForwarder;
    private final LiveWallpaper liveWallpaperForwarder;
    private final InterfaceTweaks interfaceTweaks;
    private final ExperienceTweaks experienceTweaks;
    private final Favorites favoritesForwarder;
    private final OreoShortcuts shortcutsForwarder;
    private final TagsMenu tagsMenu;
    private final Notification notificationForwarder;
    private ListPopup mainMenuPopup = null;

    // Inner classes for main menu popup
    static class MainMenuItem {
        final int id;
        final String name;

        MainMenuItem(int id, String name) {
            this.id = id;
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }
    }

    static class MainMenuAdapter extends BaseAdapter {
        final ArrayList<MainMenuItem> list = new ArrayList<>();

        @Override
        public int getCount() {
            return list.size();
        }

        @Override
        public MainMenuItem getItem(int position) {
            return list.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            MainMenuItem item = getItem(position);
            convertView = LayoutInflater.from(parent.getContext()).inflate(R.layout.popup_list_item, parent, false);
            TextView textView = convertView.findViewById(android.R.id.text1);
            textView.setText(item.toString());
            return convertView;
        }

        public void add(MainMenuItem item) {
            list.add(item);
        }

        @Override
        public boolean areAllItemsEnabled() {
            return true;
        }

        @Override
        public boolean isEnabled(int position) {
            return true;
        }
    }

    public ForwarderManager(MainActivity mainActivity) {
        super(mainActivity);

        this.widgetsForwarder = new Widgets(mainActivity);
        this.interfaceTweaks = new InterfaceTweaks(mainActivity);
        this.liveWallpaperForwarder = new LiveWallpaper(mainActivity);
        this.experienceTweaks = new ExperienceTweaks(mainActivity);
        this.favoritesForwarder = new Favorites(mainActivity);
        this.shortcutsForwarder = new OreoShortcuts(mainActivity);
        this.notificationForwarder = new Notification(mainActivity);
        this.tagsMenu = new TagsMenu(mainActivity);
    }

    public void onCreate() {
        favoritesForwarder.onCreate();
        widgetsForwarder.onCreate();
        interfaceTweaks.onCreate();
        experienceTweaks.onCreate();
        shortcutsForwarder.onCreate();
        tagsMenu.onCreate();

    }

    public void onStart() {
        widgetsForwarder.onStart();
    }

    public void onResume() {
        interfaceTweaks.onResume();
        experienceTweaks.onResume();
        notificationForwarder.onResume();
        tagsMenu.onResume();
    }

    public void onPause() {
        notificationForwarder.onPause();
    }

    public void onStop() {
    }

    public void onGlobalLayout() {
        experienceTweaks.onGlobalLayout();
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        widgetsForwarder.onActivityResult(requestCode, resultCode, data);
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        return widgetsForwarder.onOptionsItemSelected(item);
    }

    public void onCreateContextMenu(ContextMenu menu) {
        widgetsForwarder.onCreateContextMenu(menu);
    }

    public boolean onTouch(View view, MotionEvent event) {
        experienceTweaks.onTouch(event);
        return liveWallpaperForwarder.onTouch(view, event);
    }

    public void onWindowFocusChanged(boolean hasFocus) {
        experienceTweaks.onWindowFocusChanged(hasFocus);
    }

    public void onDataSetChanged() {
        widgetsForwarder.onDataSetChanged();
        favoritesForwarder.onDataSetChanged();
    }

    public void updateSearchRecords(String query) {
        favoritesForwarder.updateSearchRecords(query);
        experienceTweaks.updateSearchRecords(query);
    }

    public void onFavoriteChange() {
        favoritesForwarder.onFavoriteChange();
    }

    public void onDisplayKissBar(boolean display) {
        experienceTweaks.onDisplayKissBar(display);
    }

    public boolean onMenuButtonClicked(View menuButton) {
        if (tagsMenu.isTagMenuEnabled()) {
            mainActivity.registerPopup(tagsMenu.showMenu(menuButton));
            return true;
        }
        // Show main menu as custom popup for consistent styling
        mainActivity.registerPopup(showMainMenu(menuButton));
        return true;
    }

    private ListPopup showMainMenu(final View anchor) {
        if (mainMenuPopup != null) {
            mainMenuPopup.dismiss();
            mainMenuPopup = null;
        }

        Context context = anchor.getContext();
        mainMenuPopup = new ListPopup(context);
        MainMenuAdapter menuAdapter = new MainMenuAdapter();

        // Add menu items matching menu_main.xml
        menuAdapter.add(new MainMenuItem(R.id.preferences, context.getString(R.string.activity_setting)));
        menuAdapter.add(new MainMenuItem(R.id.wallpaper, context.getString(R.string.menu_wallpaper)));

        // Only show add widget if history is hidden
        if (prefs.getBoolean("history-hide", false)) {
            menuAdapter.add(new MainMenuItem(R.id.add_widget, context.getString(R.string.menu_widget_add)));
        }

        menuAdapter.add(new MainMenuItem(R.id.settings, context.getString(R.string.menu_settings)));

        // Add private space item if available
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            UserHandle privateUser = getPrivateUser();
            if (privateUser != null) {
                String title;
                if (isPrivateSpaceUnlocked(privateUser)) {
                    title = context.getString(R.string.lock_private_space);
                } else {
                    title = context.getString(R.string.unlock_private_space);
                }
                menuAdapter.add(new MainMenuItem(R.id.private_space, title));
            }
        }

        mainMenuPopup.setAdapter(menuAdapter);
        mainMenuPopup.setOnItemClickListener((adapter, view, position) -> {
            Object adapterItem = adapter.getItem(position);
            if (adapterItem instanceof MainMenuItem) {
                MainMenuItem item = (MainMenuItem) adapterItem;
                handleMainMenuClick(item.id);
            }
            if (mainMenuPopup != null) {
                mainMenuPopup.dismiss();
                mainMenuPopup = null;
            }
        });

        mainMenuPopup.show(anchor, 1.0f);
        return mainMenuPopup;
    }

    private void handleMainMenuClick(int itemId) {
        if (itemId == R.id.settings) {
            mainActivity.startActivity(new Intent(Settings.ACTION_SETTINGS));
        } else if (itemId == R.id.wallpaper) {
            mainActivity.hideKeyboard();
            Intent intent = new Intent(Intent.ACTION_SET_WALLPAPER);
            mainActivity.startActivity(Intent.createChooser(intent, mainActivity.getString(R.string.menu_wallpaper)));
        } else if (itemId == R.id.preferences) {
            mainActivity.startActivity(new Intent(mainActivity, SettingsActivity.class));
        } else if (itemId == R.id.private_space) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                UserHandle user = getPrivateUser();
                if (user != null) {
                    UserManager userManager = (UserManager) mainActivity.getSystemService(Context.USER_SERVICE);
                    if (isPrivateSpaceUnlocked(user)) {
                        userManager.requestQuietModeEnabled(true, user);
                    } else {
                        userManager.requestQuietModeEnabled(false, user);
                    }
                }
            }
        } else if (itemId == R.id.add_widget) {
            widgetsForwarder.showWidgetPicker();
        }
    }

    private UserHandle getPrivateUser() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            UserManager userManager = (UserManager) mainActivity.getSystemService(Context.USER_SERVICE);
            for (UserHandle user : userManager.getUserProfiles()) {
                if (!user.equals(android.os.Process.myUserHandle())) {
                    return user;
                }
            }
        }
        return null;
    }

    private boolean isPrivateSpaceUnlocked(UserHandle user) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            UserManager userManager = (UserManager) mainActivity.getSystemService(Context.USER_SERVICE);
            return userManager.isUserUnlocked(user);
        }
        return false;
    }

    public void onDestroy() {
        widgetsForwarder.onDestroy();
    }
}
