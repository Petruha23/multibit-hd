package org.multibit.hd.ui.views.components;

import org.multibit.hd.ui.views.components.renderers.RAGStatusRenderer;

import javax.swing.table.DefaultTableCellRenderer;

/**
 *  <p>Factory to provide renderers to
 * <br>
 *  <ul>
 *  <li>Tables</li>
 *  </ul>
 *
 *  </p>
 *  
 */
public class Renderers {
  /**
   * Utilities have no public constructor
   */
  private Renderers() {
  }

  public static DefaultTableCellRenderer newRAGStatusRenderer() {
    return new RAGStatusRenderer();
  }

}
