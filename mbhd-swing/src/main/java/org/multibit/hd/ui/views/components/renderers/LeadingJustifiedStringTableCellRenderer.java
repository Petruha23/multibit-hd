package org.multibit.hd.ui.views.components.renderers;

import org.multibit.hd.ui.MultiBitUI;
import org.multibit.hd.ui.views.components.Labels;
import org.multibit.hd.ui.views.themes.Themes;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;

/**
 *  <p>Renderer to provide the following to tables:</p>
 *  <ul>
 *  <li>Renders dates</li>
 *  </ul>
 *  
 */
public class LeadingJustifiedStringTableCellRenderer extends DefaultTableCellRenderer {

  JLabel label;

  public static final int TABLE_BORDER = 3;

  public static final String SPACER = "   "; // 3 spaces

  public LeadingJustifiedStringTableCellRenderer() {

    label = Labels.newBlankLabel();

  }

  @Override
  public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row,
                                                 int column) {

    label.setHorizontalAlignment(SwingConstants.LEADING);
    label.setOpaque(true);
    label.setFont(label.getFont().deriveFont(MultiBitUI.TABLE_TEXT_FONT_SIZE));

    if (value != null) {
      label.setText(value.toString());
    }

    if (isSelected) {
      label.setBackground(table.getSelectionBackground());
      label.setForeground(table.getSelectionForeground());
    } else {
      label.setForeground(table.getForeground());
      if (row % 2 == 1) {
        label.setBackground(Themes.currentTheme.tableRowAltBackground());
      } else {
        label.setBackground(Themes.currentTheme.tableRowBackground());
      }
    }

    return label;
  }

}
