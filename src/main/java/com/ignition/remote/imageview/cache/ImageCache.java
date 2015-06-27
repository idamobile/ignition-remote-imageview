/* Copyright (c) 2009 Matthias Kaeppler
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ignition.remote.imageview.cache;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.*;

/**
 * Implements a cache capable of caching image files. It exposes helper methods to immediately
 * access binary image data as {@link Bitmap} objects.
 * 
 * @author Matthias Kaeppler
 * 
 */
public class ImageCache extends AbstractCache<String, Bitmap> {

    public ImageCache(int initialCapacity, long expirationInMinutes, int maxConcurrentThreads) {
        super("ImageCache", initialCapacity, expirationInMinutes, maxConcurrentThreads);
    }

    public synchronized void removeAllWithPrefix(String urlPrefix) {
        CacheHelper.removeAllWithStringPrefix(this, urlPrefix);
    }

    @Override
    public String getFileNameForKey(String imageUrl) {
        return CacheHelper.getFileNameFromUrl(imageUrl);
    }

    @Override
    public boolean containsKeyInMemory(Object key) {
        if (super.containsKeyInMemory(key)) {
            Bitmap result = super.get(key);
            if (result != null && !result.isRecycled()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public Bitmap get(Object elementKey) {
        try {
            Bitmap result = super.get(elementKey);
            if (result != null && result.isRecycled()) {
                remove(elementKey);
                return get(elementKey);
            }
            return result;
        } catch (OutOfMemoryError er) {
            Log.d(ImageCache.class.getSimpleName(), "image too large", er);
            return null;
        }
    }

    @Override
    protected Bitmap readValueFromDisk(File file) throws IOException {
        BufferedInputStream istream = new BufferedInputStream(new FileInputStream(file));
        try {
            long fileSize = file.length();
            if (fileSize > Integer.MAX_VALUE) {
                throw new IOException("Cannot read files larger than " + Integer.MAX_VALUE + " bytes");
            }
            return BitmapFactory.decodeStream(istream);
        } finally {
            if (istream != null) {
                try {
                    istream.close();
                } catch (IOException ex) {
                }
            }
        }
    }

    @Override
    protected int getSizeOf(String key, Bitmap value) {
        return value.getRowBytes();
    }

    public synchronized Bitmap getBitmap(Object elementKey) {
        return get(elementKey);
    }

    @Override
    protected void writeValueToDisk(File file, Bitmap imageData) throws IOException {
        if (imageData != null && !imageData.isRecycled()) {
            BufferedOutputStream ostream = null;
            try {
                ostream = new BufferedOutputStream(new FileOutputStream(file));
                imageData.compress(Bitmap.CompressFormat.PNG, 90, ostream);
            } finally {
                if (ostream != null) {
                    ostream.close();
                }
            }
        }
    }
}
