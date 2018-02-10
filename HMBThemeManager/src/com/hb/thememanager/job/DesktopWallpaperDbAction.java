package com.hb.thememanager.job;

import java.io.File;
import java.util.List;

import com.hb.thememanager.database.ThemeDatabaseController;
import com.hb.thememanager.listener.OnThemeLoadedListener;
import com.hb.thememanager.model.Theme;
import com.hb.thememanager.model.Wallpaper;
import com.hb.thememanager.utils.Config;

public class DesktopWallpaperDbAction extends AbsThemeDatabaseTaskAction {

	public DesktopWallpaperDbAction(OnThemeLoadedListener<Theme> loadListener,
			ThemeDatabaseController<Theme> dbController) {
		super(loadListener, dbController);
		// TODO Auto-generated constructor stub
	}
	

	public DesktopWallpaperDbAction(OnThemeLoadedListener<Theme> loadListener,
			ThemeDatabaseController<Theme> dbController, String themeFilePath) {
		super(loadListener, dbController, themeFilePath);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	protected void doJob() {
		final int flag = getActionFlag();
		final ThemeDatabaseController<Theme> dbController = getDatabaseController();
		final OnThemeLoadedListener<Theme> listener = getListener();
		if(flag == FLAG_WRITE){
			writeDesktopWallpaperIntoDb(listener, dbController);
		}else{
			readDesktopWallpaperFromDb(listener, dbController);
		}
	
	}
	
	private void readDesktopWallpaperFromDb(OnThemeLoadedListener<Theme> listener,
			ThemeDatabaseController<Theme> dbController){
		List<Theme> wallpapers = dbController.getThemes();
		if(wallpapers != null && wallpapers.size() > 0){
			for(Theme w : wallpapers){
				if(listener != null){
					listener.onThemeLoaded(Config.LoadThemeStatus.STATUS_SUCCESS, (Wallpaper)w);
				}
			}
		}
	}
	
	private void writeDesktopWallpaperIntoDb(OnThemeLoadedListener<Theme> listener,
			ThemeDatabaseController<Theme> dbController){
			final String themePath = getFilePath();
			File wallpaperFile = new File(themePath);
			if(!wallpaperFile.exists()){
				listener.onThemeLoaded(Config.LoadThemeStatus.STATUS_THEME_NOT_EXISTS, null);
				return;
			} else {
					if (wallpaperFile.isDirectory()){
						if( wallpaperFile
									.getAbsolutePath()
									.equals(Config.Wallpaper.SYSTEM_DESKTOP_WALLPAPER_PATH)) {
							File[] systemWallpapers = wallpaperFile.listFiles();
							if(systemWallpapers != null && systemWallpapers.length > 0){
								for(File sysW : systemWallpapers){
									Wallpaper wallpaper = new Wallpaper();
									wallpaper.type = Wallpaper.WALLPAPER;
									wallpaper.isSystemTheme = Wallpaper.SYSTEM_THEME;
									wallpaper.applyStatus = Wallpaper.UN_APPLIED;
									wallpaper.loaded = Wallpaper.LOADED;
									wallpaper.themeFilePath = sysW.getAbsolutePath();
									wallpaper.loadedPath = sysW.getAbsolutePath();
									dbController.insertTheme(wallpaper);
								}
							}

					}else{
						listener.onThemeLoaded(Config.LoadThemeStatus.STATUS_THEME_FILE_ERROR, null);
						return;
					}
				}else{
					
				}
			}
			if(listener != null){
				listener.initialFinished(true,Theme.WALLPAPER);
			}
	}

}
