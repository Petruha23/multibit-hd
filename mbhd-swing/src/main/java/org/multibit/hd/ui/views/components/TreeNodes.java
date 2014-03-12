package org.multibit.hd.ui.views.components;

import org.multibit.hd.ui.languages.MessageKey;
import org.multibit.hd.ui.languages.Languages;
import org.multibit.hd.ui.views.screens.Screen;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * <p>Utility to provide the following to UI:</p>
 * <ul>
 * <li>Provision of localised tree nodes</li>
 * </ul>
 *
 * @since 0.0.1
 *  
 */
public class TreeNodes {

  /**
   * Utilities have no public constructor
   */
  private TreeNodes() {
  }

  /**
   * @param text         The custom text for a mutable node
   * @param detailScreen The detail screen to show if selected
   *
   * @return A new tree node for use in the sidebar
   */
  public static DefaultMutableTreeNode newSidebarTreeNode(String text, Screen detailScreen) {

    SidebarNodeInfo nodeInfo = new SidebarNodeInfo(text, detailScreen);

    return new DefaultMutableTreeNode(nodeInfo);
  }

  /**
   * @param messageKey   The message key for a fixed leaf node
   * @param detailScreen The detail screen to show if selected
   *
   * @return A new tree node for use in the sidebar
   */
  public static DefaultMutableTreeNode newSidebarTreeNode(MessageKey messageKey, Screen detailScreen) {

    SidebarNodeInfo nodeInfo = new SidebarNodeInfo(Languages.safeText(messageKey), detailScreen);

    return new DefaultMutableTreeNode(nodeInfo);
  }

}
