package com.redcareditor.mate.colouring.swt;

import java.util.ArrayList;
import java.util.Comparator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CaretEvent;
import org.eclipse.swt.custom.CaretListener;
import org.eclipse.swt.custom.LineStyleEvent;
import org.eclipse.swt.custom.LineBackgroundEvent;
import org.eclipse.swt.custom.LineBackgroundListener;
import org.eclipse.swt.custom.LineStyleListener;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.ImageData;
import org.eclipse.swt.graphics.PaletteData;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Caret;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import com.redcareditor.mate.DoublePattern;
import com.redcareditor.mate.MateText;
import com.redcareditor.mate.Scope;
import com.redcareditor.mate.SinglePattern;
import com.redcareditor.mate.colouring.Colourer;
import com.redcareditor.theme.Theme;
import com.redcareditor.theme.ThemeSetting;
import com.redcareditor.util.swt.ColourUtil;

public class SwtColourer implements Colourer {
	private Theme theme;
	private MateText mateText;

	private int highlightedLine = 0;
	private StyledText control;

	public SwtColourer(MateText mt) {
		mateText = mt;
		
		control = mateText.getControl();
		
		this.control.addPaintListener(new MarginPaintListener(mateText));
		this.control.addLineStyleListener(new LineStyleListener() {
			public void lineGetStyle(LineStyleEvent event) {
				colourLine(event);
			}
		});
		this.control.addLineBackgroundListener(new LineBackgroundListener() {
			public void lineGetBackground(LineBackgroundEvent event) {
				int eventLine = control.getLineAtOffset(event.lineOffset);
				int caretLine = control.getLineAtOffset(control.getCaretOffset());
				if (eventLine == caretLine) {
					event.lineBackground = ColourUtil.getColour(globalLineBackground());
					highlightedLine = eventLine;
				}
			}
		});
		
		control.addCaretListener(new CaretListener() {
			public void caretMoved(CaretEvent e) {
				int line = control.getLineAtOffset(e.caretOffset);
				if (line == highlightedLine) return;
				if (Math.abs(highlightedLine - line) > 1) {
					redrawLines(highlightedLine);
				}
				redrawLines(line);
			}
		});
	}
	
	private void redrawLines(int line) {
		int maxPossibleLine = control.getLineCount() - 1;
		int startLine = (line - 1 > maxPossibleLine ? maxPossibleLine : line - 1);
		startLine = (startLine < 0 ? 0 : startLine);
		int startOffset  = control.getOffsetAtLine(startLine);
		
		int endingOffset = startOffset + 1;
		if (line + 1 < maxPossibleLine) {
			endingOffset = control.getOffsetAtLine(line + 2);
		} else {
			endingOffset = control.getText().length() - 1;
		}
		
		control.redrawRange(startOffset, endingOffset - startOffset, false);
	}
	
	public void setTheme(Theme theme) {
		this.theme = theme;
		theme.initForUse();
		setGlobalColours();
	}

	public void setGlobalColours() {
		if (theme != null) {
			setMateTextColours();
			setCaretColour();
		}
	}

	private void setCaretColour() {
		Caret caret = control.getCaret();
		Rectangle bounds = caret.getBounds();
		int width = bounds.width;
		int height = bounds.height;
		caret = new Caret(control, SWT.NONE);
		Display display = Display.getCurrent();
		// System.out.printf("caret colour: %s %d %d\n", globalColour("caret"), width, height);
		String caretColourString = bareGlobalColour("caret");
		Color caretColour = ColourUtil.getColour(caretColourString);
		Color white = display.getSystemColor(SWT.COLOR_WHITE);
		Color black = display.getSystemColor(SWT.COLOR_BLACK);
		String backgroundColourString = globalBackground();
		int red = Integer.parseInt(backgroundColourString.substring(1, 3), 16) ^
					Integer.parseInt(caretColourString.substring(1, 3), 16);
		int green = Integer.parseInt(backgroundColourString.substring(3, 5), 16) ^
						Integer.parseInt(caretColourString.substring(3, 5), 16);
		int blue = Integer.parseInt(backgroundColourString.substring(5, 7), 16) ^
						Integer.parseInt(caretColourString.substring(5, 7), 16);
		PaletteData palette = new PaletteData (
			new RGB [] {
				new RGB (0, 0, 0),
				new RGB (red, green, blue),
				new RGB (0xFF, 0xFF, 0xFF),
			});
		ImageData maskData = new ImageData (1, height, 2, palette);
		for (int y=0; y < height; y++)
			maskData.setPixel(0, y, 1);
		Image image = new Image (display, maskData);
		caret.setImage(image);
		control.setCaret(caret);
	}

	private void setMateTextColours() {
		control.setBackground(ColourUtil.getColour(globalBackground()));
		control.setForeground(ColourUtil.getColour(globalForeground()));
		int currentLine = control.getLineAtOffset(control.getCaretOffset());
//		int startLine = JFaceTextUtil.getPartialTopIndex(control);
	//	int endLine = JFaceTextUtil.getPartialBottomIndex(control);
		for (int i = 0; i < control.getLineCount(); i ++)
			control.setLineBackground(i, 1, ColourUtil.getColour(globalBackground()));
		control.setLineBackground(currentLine, 1, ColourUtil.getColour(globalLineBackground()));
		if (mateText.getCompositeRuler() != null) {
    	    Composite rulerControl = (Composite) mateText.getCompositeRuler().getControl();
	        for (Control c : rulerControl.getChildren()) {
            	c.setBackground(ColourUtil.getColour(globalLineBackground()));
        	    c.setForeground(ColourUtil.getColour(globalForeground()));
    	    }
	        rulerControl.getParent().setBackground(ColourUtil.getColour(globalLineBackground()));
		}
		mateText.setGutterBackground(ColourUtil.getColour(globalLineBackground()));
		mateText.setGutterForeground(ColourUtil.getColour(globalForeground()));
	}

	public Theme getTheme() {
		return theme;
	}

	private ThemeSetting globalThemeSetting() {
		if (mateText.parser != null && mateText.parser.grammar != null && theme != null) {
			return theme.findSetting(mateText.parser.grammar.scopeName, false, null);
		}
		else {
			return new ThemeSetting();
		}
	}

	private String globalBackground() {
		ThemeSetting globalSetting = globalThemeSetting();
		if (globalSetting.background == null) {
			return bareGlobalColour("background");
		}
		else {
			return globalSetting.background;
		}
	}

	private String globalForeground() {
		ThemeSetting globalSetting = globalThemeSetting();
		if (globalSetting.foreground == null) {
			return bareGlobalColour("foreground");
		}
		else {
			return globalSetting.foreground;
		}
	}

	private String globalLineBackground() {
		return ColourUtil.mergeColour(globalBackground(), bareGlobalColour("lineHighlight"));
	}

	private String bareGlobalColour(String name) {
		if (theme == null)
			return "#FFFFFF";
		String colour = theme.globalSettings.get(name);
		if (isColorDefined(colour)) {
			return colour;
		} else {
			return "#FFFFFF";
		}
	}

	private boolean themeHasMarginColours() {
		return (theme.globalSettings.get("marginForeground") != null &&
				theme.globalSettings.get("marginBackground") != null);
	}

	private Color globalMarginForeground() {
		return ColourUtil.getColour(theme.globalSettings.get("marginForeground"));
	}

    private Color globalMarginBackground() {
        return ColourUtil.getColour(theme.globalSettings.get("marginBackground"));
    }

	private boolean isColorDefined(String colour) {
		return colour != null && !(colour.length() == 0);
	}

	public class StyleRangeComparator implements Comparator {

		public int compare(Object o1, Object o2) {
			StyleRange s1 = (StyleRange) o1;
			StyleRange s2 = (StyleRange) o2;
			if (s1.start < s2.start) {
				return -1;
			} else {
				if (s1.start > s2.start) {
					return 1;
				}
				else {
			 		if (s1.length < s2.length) {
						return -1;
					}
					else {
						if (s1.length > s2.length) {
							return 1;
						}
						else {
							return 0;
						}
					}
				}
			}
		}

		public boolean equals(Object obj) {
			return false;
		}
	}

	private void colourLine(LineStyleEvent event) {
		if (theme == null)
			return;
		if (!mateText.shouldColour())
			return;
		int eventLine = mateText.getControl().getLineAtOffset(event.lineOffset);
		// System.out.printf("c%d, ", eventLine);

		// ArrayList<Scope> scopes = mateText.parser.root.scopesOnLine(eventLine);
		int startLineOffset = event.lineOffset;
		int endLineOffset;

		if (eventLine >= mateText.getControl().getLineCount() - 1)
			endLineOffset = mateText.getControl().getCharCount();
		else
			endLineOffset = mateText.getControl().getOffsetAtLine(eventLine + 1);

		ArrayList<Scope> scopes = mateText.parser.root.scopesBetween(startLineOffset, endLineOffset);
		//System.out.printf("[Color] colouring %d (%d-%d) n%d\n", eventLine, startLineOffset, endLineOffset, scopes.size());

		// System.out.printf("[Color] got to colour %d scopes\n", scopes.size());
		ArrayList<StyleRange> styleRanges = new ArrayList<StyleRange>();
		for (Scope scope : scopes) {
			// System.out.printf("[Color] scope: %s\n", scope.name);
			if (scope.parent == null) {
				continue;
			}
			if (scope.name == null && scope.pattern != null
					&& (scope.pattern instanceof SinglePattern || ((DoublePattern) scope.pattern).contentName == null)) {
				continue;
			}
			addStyleRangeForScope(styleRanges, scope, false, event);
			if (scope.pattern instanceof DoublePattern && ((DoublePattern) scope.pattern).contentName != null && scope.isCapture == false)
				addStyleRangeForScope(styleRanges, scope, true, event);
			// printStyleRanges(styleRanges);
		}
		int tabWidth = mateText.getControl().getTabs();
		addMarginColumnStyleRange(styleRanges, event, tabWidth);

		event.styles = (StyleRange[]) styleRanges.toArray(new StyleRange[0]);
	}

	private void printStyleRanges(ArrayList<StyleRange> styleRanges) {
		System.out.printf("[");
		for (StyleRange r : styleRanges) {
			System.out.printf("%s, ", r);
		}
		System.out.printf("]\n");
	}

	private void addStyleRangeForScope(ArrayList<StyleRange> styleRanges, Scope scope, boolean inner, LineStyleEvent event) {
		StyleRange styleRange = new StyleRange();

		ThemeSetting setting = null;
		ThemeSetting excludeSetting = null;
		if (scope.parent != null)
			excludeSetting = scope.parent.themeSetting;
		setting = theme.settingsForScope(scope, inner, null);

		int startLineOffset = event.lineOffset;
		int endLineOffset   = startLineOffset + event.lineText.length();

		if (inner) {
			styleRange.start = Math.max(scope.getInnerStart().getOffset(), startLineOffset);
			styleRange.length = Math.min(scope.getInnerEnd().getOffset() - styleRange.start,
										 event.lineText.length() - styleRange.start + startLineOffset);
		} else {
			styleRange.start = Math.max(scope.getStart().getOffset(), startLineOffset);
			styleRange.length = Math.min(scope.getEnd().getOffset() - styleRange.start,
										 event.lineText.length() - styleRange.start + startLineOffset);
		}
		if (styleRange.length == 0)
			return;
		if (setting != null) {
			setStyleRangeProperties(scope, setting, styleRange);
			addStyleRangeWithoutOverlaps(styleRanges, styleRange);
			//System.out.printf("[Color] style range (%d, %d) %s\n", styleRange.start, styleRange.length, styleRange.toString());
		}
	}

	private void addMarginColumnStyleRange(ArrayList<StyleRange> styleRanges, LineStyleEvent event, int tabWidth) {
		int marginColumn = mateText.getMarginColumn();

		if (marginColumn == -1 || !themeHasMarginColours())
			return;

		int startLineOffset = event.lineOffset;
		int endLineOffset   = startLineOffset + event.lineText.length();
		int maxColumn       = MateText.columnOfLineOffset(event.lineText, endLineOffset - startLineOffset, tabWidth);

		if (maxColumn <= marginColumn)
			return;

		StyleRange styleRange = new StyleRange();

		int offsetOfColumn = MateText.lineOffsetOfColumn(event.lineText, marginColumn, tabWidth);
		styleRange.start = startLineOffset + offsetOfColumn;
		styleRange.length = endLineOffset - styleRange.start;

		styleRange.background = globalMarginBackground();
		styleRange.foreground = globalMarginForeground();

		addStyleRangeWithoutOverlaps(styleRanges, styleRange);
	}

	private void addStyleRangeWithoutOverlaps(ArrayList<StyleRange> styleRanges, StyleRange styleRange) {
		if (styleRanges.size() == 0) {
			styleRanges.add(styleRange);
			return;
		}

		// there is always an overlapping StyleRange because the document root scope is always in here
		int indexOfParent = indexOfOverlappingStyleRange(styleRanges, styleRange);
		if (indexOfParent == -1) {
			styleRanges.add(styleRange);
			return;
		}

		StyleRange parentStyleRange = styleRanges.get(indexOfParent);

		int parentStart = parentStyleRange.start;
		int parentEnd   = parentStyleRange.start + parentStyleRange.length;
		int childStart  = styleRange.start;
		int childEnd    = styleRange.start + styleRange.length;

		//System.out.printf("parent %d-%d, child: %d-%d\n", parentStart, parentEnd, childStart, childEnd);

		// *-----*
		// *-----*
		if (parentStart == childStart && parentEnd == childEnd) {
			styleRangeCopyValues(parentStyleRange, styleRange);
			return;
		}

		// *------*
		// *--*
		if (childStart == parentStart) {
			parentStyleRange.start = childEnd;
			parentStyleRange.length -= styleRange.length;
			styleRanges.add(indexOfParent, styleRange);
			return;
		}

		// *------*
		//    *---*
		if (childEnd == parentEnd) {
			parentStyleRange.length = childStart - parentStart;
			styleRanges.add(indexOfParent + 1, styleRange);
			return;
		}

		// *----------*
		//    *---*
		parentStyleRange.length = childStart - parentStart;
		styleRanges.add(indexOfParent + 1, styleRange);
		StyleRange newStyleRange = new StyleRange();
		newStyleRange.start = childEnd;
		newStyleRange.length = parentEnd - childEnd;
		styleRangeCopyValues(newStyleRange, parentStyleRange);
		styleRanges.add(indexOfParent + 2, newStyleRange);
	}

	private void styleRangeCopyValues(StyleRange target, StyleRange source) {
		target.fontStyle = source.fontStyle;
		target.foreground = source.foreground;
		target.background = source.background;
		target.underline = source.underline;
	}

	private int indexOfOverlappingStyleRange(ArrayList<StyleRange> styleRanges, StyleRange styleRange) {
		int i = 0;
		for (StyleRange possibleParent : styleRanges) {
			if (possibleParent.start < styleRange.start + styleRange.length &&
				possibleParent.start + possibleParent.length > styleRange.start)
				return i;
			i++;
		}
		return -1;
	}

	private void setStyleRangeProperties(Scope scope, ThemeSetting setting, StyleRange styleRange) {
		String fontStyle = setting.fontStyle;
		if (fontStyle != null) {
			// TODO: make this support "bold italic" etc.
			if (fontStyle.equals("italic")) {
				styleRange.fontStyle = SWT.ITALIC;
			}
			if (fontStyle.equals("bold")) {
				styleRange.fontStyle = SWT.BOLD;
			}
			if (fontStyle.equals("underline")) {
				styleRange.underline = true;
			}
		}

		String background = setting.background;
		// System.out.printf("[Color] scope background: %s\n", background);
		String mergedBgColour;
		String parentBg = globalBackground();
		//System.out.printf("		   global background: %s\n", parentBg);
		if (background != null && background != "") {
			mergedBgColour = ColourUtil.mergeColour(parentBg, background);
			if (mergedBgColour != null) {
				scope.bgColour = mergedBgColour;
				styleRange.background = ColourUtil.getColour(mergedBgColour);
				// System.out.printf("[Color] background = %s\n", mergedBgColour);
			}
		} else {
			mergedBgColour = parentBg;
		}
		// stdout.printf("		  merged_bg_colour:	 %s\n", merged_bg_colour);
		String foreground = setting.foreground;
		// System.out.printf("[Color] scope foreground: %s\n", foreground);
		String parentFg = scope.nearestForegroundColour();
		if (parentFg == null) {
			parentFg = globalForeground();
			// stdout.printf("		  global foreground:		%s\n",
			// parent_fg);
		}
		if (foreground != null && foreground != "") {
			String mergedFgColour;
			if (parentFg != null && !scope.isCapture)
				mergedFgColour = ColourUtil.mergeColour(parentFg, foreground);
			else
				mergedFgColour = foreground;
			if (mergedFgColour != null) {
				scope.fgColour = mergedFgColour;
				styleRange.foreground = ColourUtil.getColour(mergedFgColour);
			}
			// stdout.printf("		 merged_fg_colour: %s\n", merged_fg_colour);
		}
		// stdout.printf("\n");
	}

}
