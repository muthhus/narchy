/*******************************************************************************
 * Copyright (c) 2016 comtel inc.
 *
 * Licensed under the Apache License, version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 *******************************************************************************/
package spacegraph.net.vnc.rfb.render.rect;

import spacegraph.net.vnc.rfb.codec.Encoding;

public abstract class ImageRect {

    public final int x;
    public final int y;
    public final int width;
    public final int height;

    public ImageRect(int x, int y, int width, int height) {
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    /**
     * @return the image {@link Encoding} type
     */
    public abstract Encoding getEncoding();


    /**
     * Release the image buffer
     *
     * @return if successful
     */
    public boolean release() {
        return true;
    }

    @Override
    public String toString() {
        return "ImageRect [x=" + x + ", y=" + y + ", width=" + width + ", height=" + height + ']';
    }

}
