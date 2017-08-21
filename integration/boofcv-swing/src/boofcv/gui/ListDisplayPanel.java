/*
 * Copyright (c) 2011-2016, Peter Abeles. All Rights Reserved.
 *
 * This file is part of BoofCV (http://boofcv.org).
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package boofcv.gui;

import boofcv.gui.image.ImagePanel;
import boofcv.gui.image.ScaleOptions;
import boofcv.io.image.ConvertBufferedImage;
import boofcv.struct.image.ImageBase;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * Displays a list of items and their respective data.
 *
 * @author Peter Abeles
 */
public class ListDisplayPanel extends JPanel implements ListSelectionListener  {

	private static final int LIST_WIDTH = 150;
	private static final int BUTTON_POSITION_X = 250;
	private static final int BUTTON_POSITION_Y = 425;
	private static final int BUTTON_SIZE_WIDTH = 100;
	private static final int BUTTON_SIZE_HEIGHT = 40;

	final List<JPanel> panels = new ArrayList<>();
	private JPanel bodyPanel;
	private JList listPanel;
	int selectedIndex = 0;
	JButton jButton = new JButton("Auswählen");
	JButton jButtonHose = new JButton("Auswählen");
	JButton jButtonSchuhe = new JButton("Auswählen!");

	DefaultListModel listModel = new DefaultListModel();

	JScrollPane scroll;

	// stores the size the center element of body should be to contain all the images without resizing
	private int bodyWidth,bodyHeight;

	public ListDisplayPanel() {
		setLayout(new BorderLayout());

		listPanel = new JList(listModel);
		listPanel.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		listPanel.setSelectedIndex(0);
		listPanel.addListSelectionListener(this);
		scroll = new JScrollPane(listPanel);
		scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		jButtonSchuhe.setBounds(BUTTON_POSITION_X,BUTTON_POSITION_Y, BUTTON_SIZE_WIDTH, BUTTON_SIZE_HEIGHT);
		jButton.setBounds(BUTTON_POSITION_X,BUTTON_POSITION_Y,BUTTON_SIZE_WIDTH,BUTTON_SIZE_HEIGHT);
		jButtonHose.setBounds(BUTTON_POSITION_X,BUTTON_POSITION_Y,BUTTON_SIZE_WIDTH,BUTTON_SIZE_HEIGHT);
		jButtonSchuhe.setVisible(false);
		jButtonHose.setVisible(false);
		jButton.setVisible(true);
		bodyPanel = new JPanel();
		bodyPanel.setPreferredSize(new Dimension(550, 500));
		bodyPanel.add(jButtonSchuhe);
		bodyPanel.add(jButtonHose);
		bodyPanel.add(jButton);
		bodyPanel.setLayout(new BorderLayout());
		bodyPanel.add(scroll,BorderLayout.WEST);
		add(bodyPanel);
	}
	public JButton getJButtonSchuhe(){
		return this.jButtonSchuhe;
	}
	public JButton getJButtonHose(){
		return this.jButtonHose;
	}

	public JButton getJButton(){
		return this.jButton;
	}
	/*
	public int addButtonClick() {
		jButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				selectedIndex = listPanel.getSelectedIndex();
				System.out.println(selectedIndex);

			}
		});
	 return selectedIndex;}*/


	public synchronized void reset() {
		panels.clear();

		if( SwingUtilities.isEventDispatchThread() ) {
			listModel.removeAllElements();
		} else {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					listModel.removeAllElements();
				}
			});
		}
//		bodyPanel.requestFocus();
	}

	public int getListWidth(){
		return (int)listPanel.getPreferredSize().getWidth();
	}

	public void addImage( ImageBase image , String name ) {
		BufferedImage buff = ConvertBufferedImage.convertTo(image, null, true);
		addImage(buff, name, ScaleOptions.DOWN);
	}

	/**
	 * Displays a new image in the list.
	 *
	 * @param image The image being displayed
	 * @param name Name of the image.  Shown in the list.
	 */
	public void addImage( BufferedImage image , String name) {
		addImage(image, name, ScaleOptions.DOWN);
	}

	public void addImage( BufferedImage image , String name , ScaleOptions scaling) {
		addItem(new ImagePanel(image, scaling), name);
	}

	/**
	 * Displays a new JPanel in the list.
	 *
	 * @param panel The panel being displayed
	 * @param name Name of the image.  Shown in the list.
	 */
	public synchronized void addItem( final JPanel panel , final String name ) {

		Dimension panelD = panel.getPreferredSize();

		// make the preferred size large enough to hold all the images
		bodyWidth = (int)Math.max(bodyWidth, panelD.getWidth());
		bodyHeight = (int)Math.max(bodyHeight,panelD.getHeight());
		panel.setBackground(Color.WHITE);
		panels.add(panel);
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				listModel.addElement(name);
				if (listModel.size() == 1) {
					listPanel.setSelectedIndex(0);
				}
				// update the list's size
				Dimension d = listPanel.getMinimumSize();
				d.setSize(LIST_WIDTH, listPanel.getHeight());
				listPanel.setPreferredSize(new Dimension(d.width + scroll.getVerticalScrollBar().getWidth(), d.height));

				// make sure it's preferred size is up to date
				Component old = ((BorderLayout) bodyPanel.getLayout()).getLayoutComponent(BorderLayout.CENTER);
				if (old != null)
					old.setPreferredSize(new Dimension(bodyWidth, bodyHeight));

				validate();
			}
		});
	}
	public int getImage(){
		listPanel.getSelectedIndex();
		return listPanel.getSelectedIndex();
	}

	@Override
	public synchronized void valueChanged(ListSelectionEvent e) {
		if( e.getValueIsAdjusting() )
			return;

		final int index = listPanel.getSelectedIndex();
		if( index >= 0 ) {
			removeCenterBody();
			JPanel p = panels.get(index);
			p.setPreferredSize(new Dimension(bodyWidth,bodyHeight));
			bodyPanel.add(p, BorderLayout.CENTER);
			bodyPanel.validate();
			bodyPanel.repaint();
		}
	}

	private void removeCenterBody() {
		Component old = ((BorderLayout)bodyPanel.getLayout()).getLayoutComponent(BorderLayout.CENTER);
		if( old != null ) {
			bodyPanel.remove(old);
		}
	}
}
