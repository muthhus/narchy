/**
 * L2FProd Common v9.2 License.
 *
 * Copyright 2005 - 2009 L2FProd.com
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
package automenta.vivisect.swing.property.swing.renderer;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * A renderer for Date.
 *  
 * @author Ricardo Lopes
 */
public class DateRenderer extends DefaultCellRenderer {

  private final DateFormat dateFormat;

  public DateRenderer() {
    this(DateFormat.getDateInstance(DateFormat.SHORT));
  }

  public DateRenderer(String formatString) {
    this(formatString, Locale.getDefault());
  }

  public DateRenderer(Locale l) {
    this(DateFormat.getDateInstance(DateFormat.SHORT, l));
  }

  public DateRenderer(String formatString, Locale l) {
    this(new SimpleDateFormat(formatString, l));
  }

  public DateRenderer(DateFormat dateFormat) {
    this.dateFormat = dateFormat;
  }

  @Override
  public void setValue(Object value) {
    if (value == null) {
      setText("");
    } else {
      //noinspection UseOfObsoleteDateTimeApi
      setText(dateFormat.format((Date)value));
    }
  }
  
}