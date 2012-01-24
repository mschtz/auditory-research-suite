package edu.mcmaster.maplelab.av;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.text.NumberFormat;
import java.util.*;
import java.util.List;

import javax.swing.*;

import org.apache.commons.lang3.StringUtils;

import net.miginfocom.swing.MigLayout;
import edu.mcmaster.maplelab.av.datamodel.*;
import edu.mcmaster.maplelab.av.media.*;
import edu.mcmaster.maplelab.av.media.MediaParams.MediaParamValue;
import edu.mcmaster.maplelab.av.media.MediaType.MediaWrapper;
import edu.mcmaster.maplelab.av.media.animation.*;
import edu.mcmaster.maplelab.common.LogContext;
import edu.mcmaster.maplelab.common.datamodel.DurationEnum;
import edu.mcmaster.maplelab.common.gui.DemoGUIPanel;
import edu.mcmaster.maplelab.common.gui.FileBrowseField;

public abstract class AVDemoGUIPanel<T extends AVTrial<?>> extends DemoGUIPanel<AVSession<?, T, ?>, T> {
	
    private FilePathUpdater _fUpdater = new FilePathUpdater();

	private FileBrowseField _audFile;
	private FileBrowseField _visFile;
	private FileBrowseField _vidFile;
	
	private JFormattedTextField _delayText;
	private JSpinner _numPts;
	private JCheckBox _connect;
	private JCheckBox _loop;
	private JCheckBox _useVideo;
	
	private JButton _startButton;
	
	private AnimationRenderer _renderer;
	private Boolean _video = null;

	private JFrame _testFrame;
	private AnimationPanel _aniPanel;
	private VideoPanel _vidPanel;
	
	private Map<MediaType<?>, Map<String, JComboBox> > _paramSelectors = new HashMap<MediaType<?>, Map<String,JComboBox>>();
	
	
	//read data from user entries and create a trial
	
	public AVDemoGUIPanel(AVSession<?, T, ?> session) {
		super(session);
		setLayout(new MigLayout("", "[][]30px[][]30px[][]30px[][]", ""));

		// auditory info
		add(new JLabel("Auditory"), "split, span, gaptop 10");
		add(new JSeparator(), "growx, wrap, gaptop 10");
		
        JPanel p = genParamControls(session, MediaType.AUDIO);
        add(p, "spany 2, grow");
		
		
		add(new JLabel("Delay (ms)", 2), "right, split");
		_delayText = new JFormattedTextField(NumberFormat.getIntegerInstance());
		_delayText.setValue(new Long(0)); // new
		_delayText.setColumns(5);
		
		add(_delayText, "left, top, wrap push");
		
		// visual info
		add(new JLabel("Visual"), "newline, split, span, gaptop 10");
		add(new JSeparator(), "growx, wrap, gaptop 10");
		
        p = genParamControls(session, MediaType.ANIMATION);
        add(p, "spany 2, right");
		
		add (new JLabel("Number of dots"), "right, split 2");
		SpinnerModel model = new SpinnerNumberModel(6, 1, 20, 1);
		_numPts = new JSpinner(model);
		add(_numPts, "left");

		add(new JLabel("Connect dots w/ lines"), "right, split 2");
		_connect = new JCheckBox();
		add(_connect, "left");
		_connect.setSelected(true);

	        
		add(new JLabel("Loop"), "newline, right, split 2");
		_loop = new JCheckBox();
		add(_loop, "left");
		
		add(new JLabel("Use video"), "right, split 2");
		_useVideo = new JCheckBox();
		add(_useVideo, "left");
		//_useVideo.setEnabled(false);
		
		// files 
		add(new JLabel("Files"), "newline, split, span, gaptop 10");
		add(new JSeparator(), "growx, wrap, gaptop 10");
		
		add(new JLabel("Audio File"), "right, span, split");
		_audFile = new FileBrowseField(false);
		add(_audFile, "growx, wrap");
		
		add(new JLabel("Visual File"),"right, span, split");
		_visFile = new FileBrowseField(false);
		add(_visFile, "growx, wrap");
		
		add(new JLabel("Video File"), "right, span, split");
		_vidFile = new FileBrowseField(false);
		add(_vidFile, "growx, wrap");
		//_vidFile.setEnabled(false);
		
		p = new JPanel(new MigLayout("insets 0, fill"));
		_startButton = new JButton("Start");
		_startButton.addActionListener(new StartUpdater());
		p.add(_startButton, "center");
		add(p, "span, center, grow");
		
		_fUpdater.update();
	}
	
	private JPanel genParamControls(AVSession<?, T, ?> session, MediaType<?> type) {
	    JPanel retval = new JPanel(new MigLayout("insets 0", "", "[][fill]"));
	    
	    Map<String, JComboBox> selectorMap = _paramSelectors.get(type);
	    if(selectorMap == null) {
	        selectorMap = new HashMap<String, JComboBox>();
	        _paramSelectors.put(type, selectorMap);
	    }
	    
        List<String> params = type.getParams(session);
        for(String param : params) {
            String label = session.getString(param + ".label", param);
            retval.add(new JLabel(label), "right");
            MediaParams vals = MediaParams.getAvailableValues(param);

            JComboBox options = new JComboBox(new MediaParamsModel(vals));
            options.addActionListener(_fUpdater);
            retval.add(options, "growx, wrap");
            
            selectorMap.put(param, options);
        }
        
        return retval;
    }

    protected abstract T createTrial(AnimationSequence animationSequence,
			boolean isVideo, MediaWrapper<Playable> media, Long timingOffset,
			int animationPoints, float diskRadius, boolean connectDots);
	  
	@Override
	public synchronized T getTrial() {
		AVSession<?, ?, ?> session = getSession();
		final boolean vid = _useVideo.isSelected();
		MediaWrapper<Playable> media = vid ? MediaType.VIDEO.createDemoMedia(_vidFile.getFile(), session) :
				MediaType.AUDIO.createDemoMedia(_audFile.getFile(), session);
		try {
			AnimationSequence aniSeq = !vid ? AnimationParser.parseFile(
					_visFile.getFile(), session.getAnimationPointAspect()) : null;
			Object val = _delayText.getValue();
			Long delay = Long.valueOf(val instanceof String ? (String) val : ((Number) val).toString());
			return createTrial(aniSeq, vid, media, delay, 
					(Integer)_numPts.getValue(), session.getBaseAnimationPointSize(), 
					_connect.isSelected());
		}
		catch (FileNotFoundException ex) {
			Runnable r = new Runnable() {
				@Override
				public void run() {
					String msg = "%s file";
					if (vid) msg = String.format(msg, "Video");
					else {
						boolean vis = _visFile.getFile().exists();
						boolean aud = _audFile.getFile().exists();
						if (!aud & !vis) {
							msg = "Audio and animation files";
						}
						else {
							msg = String.format(msg, aud ? "Animation" : "Audio");
						}
					}
					
					JOptionPane.showMessageDialog(AVDemoGUIPanel.this, 
							msg + " could not be found.", 
							"Missing file(s)...", 
							JOptionPane.ERROR_MESSAGE);
					
				}
			};
			
			boolean ran = false;
			if (!SwingUtilities.isEventDispatchThread()) {
				try {
					SwingUtilities.invokeAndWait(r);
					ran = true;
				} 
				catch (Exception e) {}
			} 
			
			if (!ran) r.run();
		} 
		
		return null;
	}
	
	/**
	 * Prepare and display the demo display window.
	 */
	private void prepareNext(T trial) {
		if (_testFrame == null) {
			_testFrame = new JFrame();
			_testFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            Window w = getParentWindow();
            _testFrame.setLocation(w.getLocation().x + w.getWidth(), w.getLocation().y);

            
            _renderer = new AnimationRenderer();
            _aniPanel = new AnimationPanel(_renderer);
		}
		
    	if (trial.isVideo()) {
    	    if(_vidPanel == null) {
                _vidPanel = new VideoPanel();
    	    }
    		_vidPanel.setMovie(trial.getVideoPlayable());
    		if (_video == null || !_video) {
    			_testFrame.getContentPane().removeAll();
        		_testFrame.getContentPane().add(_vidPanel, BorderLayout.CENTER);
        		_testFrame.setTitle(_vidPanel.getClass().getSimpleName());
        		_aniPanel.stop();
    		}
    		_video = true;
    	}
    	else {
    		if (_video == null || _video) {
    			_testFrame.getContentPane().removeAll();
        		_testFrame.getContentPane().add(_aniPanel, BorderLayout.CENTER);
        		_testFrame.setTitle(_aniPanel.getClass().getSimpleName());
        		_aniPanel.start();
    		}
    		_video = false;
    	}

        _testFrame.pack();
        
        if(getSession().isOscilloscopeSensorMode()) {
            Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
            Dimension size = _testFrame.getSize();
            _testFrame.setLocation(screenSize.width - size.width, screenSize.height - size.height);
        }

        _testFrame.setVisible(true);
        
	}
	
	/**
	 * Class for updating file fields.
	 */
	private class FilePathUpdater implements ActionListener{

		@Override
		public void actionPerformed(ActionEvent e) {
			update();
		}
		
		private File fileFor(MediaType<?> type) {
            List<String> paramNames = type.getParams(getSession());
            List<MediaParamValue> selections = new ArrayList<MediaParams.MediaParamValue>(paramNames.size()); 
            for(String p : paramNames) {
                JComboBox sel = _paramSelectors.get(type).get(p);
                if(sel == null) {
                    throw new IllegalStateException("Missing combo box for parameter type " + p);
                }
                selections.add((MediaParamValue) sel.getSelectedItem());
            }
            
            File f = type.getExpectedFile(getSession(), selections);

            if(f == null) {
                String basename = type.getExpectedFilename(getSession(), selections);
                LogContext.getLogger().severe("Unable find file with form: " + StringUtils.abbreviateMiddle(basename, "...", 50) + ".*");
                // We return a file even though it's invalid so the file browser can indicate it's invalid.
                return new File(basename);
            }
            
            return f;
            
		}
		
		public void update() {
		    
		    File f = fileFor(MediaType.AUDIO);
		    _audFile.setFile(f);
		    
            f = fileFor(MediaType.ANIMATION);
            _visFile.setFile(f);
			
//			f = MediaType.VIDEO.getExpectedFile(getSession(), _pitches.getSelectedItem(),
//					_vDurations.getSelectedItem(), _aDurations.getSelectedItem());
//			if (f == null) f = getSession().getVideoDirectory();
//			_vidFile.setFile(f);
		}
	}
	
	/**
	 * StartUpdater responds to the "start" button by triggering the trial sequence.
	 * @author Catherine Elder <cje@datamininglab.com>
	 *
	 */
	private class StartUpdater implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			_startButton.setEnabled(false);
			SwingUtilities.invokeLater(new PrepareAndRun());
		}
	}
	
	private class PrepareAndRun implements Runnable {
		@Override
		public void run() {
		    try {
		        T next = getTrial();
		        prepareNext(next);
		        next.preparePlayback(getSession(), _renderer);
		        next.addPlaybackListener(new LoopListener(next));
		        next.play();
		    }
		    finally {
                _startButton.setEnabled(true);
		    }
		}
	}
	
	private class LoopListener implements TrialPlaybackListener {
		private T _trial;
		
		public LoopListener(T trial) {
			_trial = trial;
		}
		
		@Override
		public void playbackEnded() {
			SwingUtilities.invokeLater(new Runnable() {
				@Override
				public void run() {
					_trial.removePlaybackListener(LoopListener.this);
					if (_loop.isSelected()) {
						SwingUtilities.invokeLater(new PrepareAndRun());
					}
					else {
						_startButton.setEnabled(true);
					}
				}
			});
		}
	}
	
    /**
     * Adapter around MediaParams for use inside a JComboBox. 
     * 
     * @author <a href="mailto:simeon.fitch@mseedsoft.com">Simeon H.K. Fitch</a>
     * @since Jan 24, 2012
     */
    public class MediaParamsModel extends DefaultComboBoxModel {

        public MediaParamsModel(MediaParams vals) {
            super(vals.getValues().toArray());
        }
    }	

}
