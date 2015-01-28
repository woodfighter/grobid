package org.grobid.core.document;

import org.grobid.core.data.BibDataSet;
import org.grobid.core.data.BiblioItem;
import org.grobid.core.data.Date;
import org.grobid.core.data.Person;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.lang.Language;
import org.grobid.core.layout.Block;
import org.grobid.core.utilities.LanguageUtilities;
import org.grobid.core.utilities.TextUtilities;
import org.grobid.core.engines.SegmentationLabel;
import org.grobid.core.engines.FullTextParser;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Class for generating a TEI representation of a document.
 *
 * @author Patrice Lopez
 */
public class TEIFormater {
    private Document doc = null;

    private StringBuffer currentParagraph = null;
    private StringBuffer currentSection = null;
    private StringBuffer currentItem = null;
    private StringBuffer currentCitationMarker = null;
    private StringBuffer currentFigureMarker = null;
    private StringBuffer currentPage = null;
    private StringBuffer currentPageFootNote = null;
    private StringBuffer currentLabel = null;
    private StringBuffer currentFigureHead = null;
    private StringBuffer currentEquation = null;

    private Boolean inParagraph = false;

    private ArrayList<String> elements = null;

    // static variable for the position of italic and bold features in the CRF model
    private static final int ITALIC_POS = 16;
    private static final int BOLD_POS = 17;

	private static Pattern numberRef = Pattern.compile("(\\[|\\()\\d+\\w?(\\)|\\])");
    private static Pattern numberRefCompact =
            Pattern.compile("(\\[|\\()((\\d)+(\\w)?(\\-\\d+\\w?)?,\\s?)+(\\d+\\w?)(\\-\\d+\\w?)?(\\)|\\])");
    //private static Pattern numberRefVeryCompact = Pattern.compile("(\\[|\\()(\\d)+-(\\d)+(\\)|\\])");
    private static Pattern numberRefCompact2 = Pattern.compile("(\\[|\\()(\\d+)(-|‒|–|—|―|\u2013)(\\d+)(\\)|\\])");

	private static Pattern startNum = Pattern.compile("^(\\d+)(.*)");

    public TEIFormater(Document document) {
        doc = document;
    }

    public StringBuffer toTEIHeader(BiblioItem biblio,
                                    boolean withStyleSheet,
                                    String defaultPublicationStatement) {
        StringBuffer tei = new StringBuffer();
        tei.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        if (withStyleSheet) {
            tei.append("<?xml-stylesheet type=\"text/xsl\" href=\"../jsp/xmlverbatimwrapper.xsl\"?> \n");
        }
        tei.append("<TEI xmlns=\"http://www.tei-c.org/ns/1.0\" xmlns:xlink=\"http://www.w3.org/1999/xlink\" " +
                "\n xmlns:mml=\"http://www.w3.org/1998/Math/MathML\">\n");
        if (doc.getLanguage() != null) {
            tei.append("\t<teiHeader xml:lang=\"" + doc.getLanguage() +
                    "\">\n\t\t<fileDesc>\n\t\t\t<titleStmt>\n\t\t\t\t<title level=\"a\" type=\"main\">");
        } else {
            tei.append("\t<teiHeader>\n\t\t<fileDesc>\n\t\t\t<titleStmt>\n\t\t\t\t<title level=\"a\" type=\"main\">");
        }
		
		if (biblio == null) {
			// if the biblio object is null, we simply create an empty one
			biblio = new BiblioItem();
		}
		
        if (biblio.getTitle() != null) {
            tei.append(TextUtilities.HTMLEncode(biblio.getTitle()));
        }

        tei.append("</title>\n\t\t\t</titleStmt>\n");
        if ((biblio.getPublisher() != null) ||
                (biblio.getPublicationDate() != null) ||
                (biblio.getNormalizedPublicationDate() != null)) {
            tei.append("\t\t\t<publicationStmt>\n");
            if (biblio.getPublisher() != null) {
				// publisher and date under <publicationStmt> for better TEI conformance
				tei.append("\t\t\t\t<publisher>" + TextUtilities.HTMLEncode(biblio.getPublisher()) + 
					"</publisher>\n");
				
                tei.append("\t\t\t\t<availability>");
                tei.append("<p>Copyright ");
                //if (biblio.getPublicationDate() != null)
                tei.append(TextUtilities.HTMLEncode(biblio.getPublisher()) + "</p>\n");
                tei.append("\t\t\t\t</availability>\n");
            }
	        else {
	            // a dummy publicationStmt is still necessary according to TEI
	            if (defaultPublicationStatement == null) {
	                tei.append("unknown");
	            } else {
	                tei.append(defaultPublicationStatement);
	            }
				tei.append("\n");
	        }
			
            if (biblio.getNormalizedPublicationDate() != null) {
                Date date = biblio.getNormalizedPublicationDate();
                int year = date.getYear();
                int month = date.getMonth();
                int day = date.getDay();

                String when = "";
				if (year <= 9) 
					when += "000" + year;
				else if (year <= 99) 
					when += "00" + year;
				else if (year <= 999)
					when += "0" + year;
				else
					when += year;
                if (month != -1) {
					if (month <= 9) 
						when += "-0" + month;
					else 
 					   	when += "-" + month;
                    if (day != -1) {
						if (day <= 9)
							when += "-0" + day;
						else
							when += "-" + day;
                    }
                }
                tei.append("\t\t\t\t<date type=\"published\" when=\"");
                tei.append(when + "\">");
				if (biblio.getPublicationDate() != null) {
					tei.append(biblio.getPublicationDate());
				}
				else {
					tei.append(when);
				}
				tei.append("</date>\n");
            } else if (biblio.getYear() != null) {
				String when = "";
				if (biblio.getYear().length() == 1)
					when += "000" + biblio.getYear();
				else if (biblio.getYear().length() == 2)
					when += "00" + biblio.getYear();
				else if (biblio.getYear().length() == 3)
					when += "0" + biblio.getYear();
				else if (biblio.getYear().length() == 4)
					when += biblio.getYear();
				
                if (biblio.getMonth() != null) {
					if (biblio.getMonth().length() == 1)
						when += "-0" + biblio.getMonth();
					else
						when += "-" + biblio.getMonth();
                    if (biblio.getDay() != null) {
						if (biblio.getDay().length() == 1)
							when += "-0" + biblio.getDay();
						else
							when += "-" + biblio.getDay();
                    }
                }
                tei.append("\t\t\t\t<date type=\"published\" when=\"");
                tei.append(when + "\">");
				if (biblio.getPublicationDate() != null) {
					tei.append(biblio.getPublicationDate());
				}
				else {
					tei.append(when);
				}
				tei.append("</date>\n");
            } else if (biblio.getE_Year() != null) {
				String when = "";
				if (biblio.getE_Year().length() == 1)
					when += "000" + biblio.getE_Year();
				else if (biblio.getE_Year().length() == 2)
					when += "00" + biblio.getE_Year();
				else if (biblio.getE_Year().length() == 3)
					when += "0" + biblio.getE_Year();
				else if (biblio.getE_Year().length() == 4)
					when += biblio.getE_Year();
				
                if (biblio.getE_Month() != null) {
					if (biblio.getE_Month().length() == 1)
						when += "-0" + biblio.getE_Month();
					else
						when += "-" + biblio.getE_Month();
					
                    if (biblio.getE_Day() != null) {
						if (biblio.getE_Day().length() == 1)
							when += "-0" + biblio.getE_Day();
						else
							when += "-" + biblio.getE_Day();
  					 }
                }
                tei.append("\t\t\t\t<date type=\"ePublished\" when=\"");
                tei.append(when + "\">");
				if (biblio.getPublicationDate() != null) {
					tei.append(biblio.getPublicationDate());
				}
				else {
					tei.append(when);
				}
				tei.append("</date>\n");
            } else if (biblio.getPublicationDate() != null) {
                tei.append("\t\t\t\t<date type=\"published\">");
                tei.append(TextUtilities.HTMLEncode(biblio.getPublicationDate())
                        + "</date>");
            }
			tei.append("\t\t\t</publicationStmt>\n");
		}
		
        tei.append("\t\t\t<sourceDesc>\n\t\t\t\t<biblStruct>\n\t\t\t\t\t<analytic>\n");

        // authors + affiliation
        //biblio.createAuthorSet();
        //biblio.attachEmails();
        //biblio.attachAffiliations();
        tei.append(biblio.toTEIAuthorBlock(6));

        // title
        String title = biblio.getTitle();
        String language = biblio.getLanguage();
        String english_title = biblio.getEnglishTitle();
        if (title != null) {
            tei.append("\t\t\t\t\t\t<title");
            /*if ( (bookTitle == null) & (journal == null) )
					tei.append(" level=\"m\"");
		    	else */
            tei.append(" level=\"a\" type=\"main\"");
            // here check the language ?
            if (english_title == null)
                tei.append(">" + TextUtilities.HTMLEncode(title) + "</title>\n");
            else
                tei.append(" xml:lang=\"" + language + "\">" + TextUtilities.HTMLEncode(title) + "</title>\n");
        }

        boolean hasEnglishTitle = false;
        if (english_title != null) {
            // here do check the language!
            LanguageUtilities languageUtilities = LanguageUtilities.getInstance();
            Language resLang = languageUtilities.runLanguageId(english_title);

            if (resLang != null) {
                String resL = resLang.getLangId();
                if (resL.equals(Language.EN)) {
                    hasEnglishTitle = true;
                    tei.append("\t\t\t\t\t\t<title");
                    //if ( (bookTitle == null) & (journal == null) )
                    //	tei.append(" level=\"m\"");
                    //else 
                    tei.append(" level=\"a\"");
                    tei.append(" xml:lang=\"en\">").append(TextUtilities.HTMLEncode(english_title)).append("</title>\n");
                }
            }
            // if it's not something in English, we will write it anyway as note without type at the end
        }

        tei.append("\t\t\t\t\t</analytic>\n");

        if ((biblio.getJournal() != null) ||
                (biblio.getJournalAbbrev() != null) ||
                (biblio.getISSN() != null) ||
                (biblio.getISSNe() != null) ||
                (biblio.getPublisher() != null) ||
                (biblio.getPublicationDate() != null) ||
                (biblio.getVolumeBlock() != null) ||
                (biblio.getItem() == BiblioItem.Periodical) ||
                (biblio.getItem() == BiblioItem.InProceedings) ||
                (biblio.getItem() == BiblioItem.Proceedings) ||
                (biblio.getItem() == BiblioItem.InBook) ||
                (biblio.getItem() == BiblioItem.Book) ||
                (biblio.getItem() == BiblioItem.Serie) ||
                (biblio.getItem() == BiblioItem.InCollection)) {
            tei.append("\t\t\t\t\t<monogr");
            tei.append(">\n");

            if (biblio.getJournal() != null) {
                tei.append("\t\t\t\t\t\t<title level=\"j\" type=\"main\">" +
                        TextUtilities.HTMLEncode(biblio.getJournal()) + "</title>\n");
            } else if (biblio.getBookTitle() != null) {
                tei.append("\t\t\t\t\t\t<title level=\"m\">" + TextUtilities.HTMLEncode(biblio.getBookTitle())
                        + "</title>\n");
            }

            if (biblio.getJournalAbbrev() != null) {
                tei.append("\t\t\t\t\t\t<title level=\"j\" type=\"abbrev\">" +
                        TextUtilities.HTMLEncode(biblio.getJournalAbbrev()) + "</title>\n");
            }

            if (biblio.getISSN() != null) {
                tei.append("\t\t\t\t\t\t<idno type=\"ISSN\">" +
                        TextUtilities.HTMLEncode(biblio.getISSN()) + "</idno>\n");
            }

            if (biblio.getISSNe() != null) {
                if (!biblio.getISSNe().equals(biblio.getISSN()))
                    tei.append("\t\t\t\t\t\t<idno type=\"eISSN\">" +
                            TextUtilities.HTMLEncode(biblio.getISSNe()) + "</idno>\n");
            }

            if (biblio.getEvent() != null) {
                // TBD	
            }

            // in case the booktitle corresponds to a proceedings, we can try to indicate the meeting title
            String meeting = biblio.getBookTitle();
            boolean meetLoc = false;
            if (biblio.getEvent() != null)
                meeting = biblio.getEvent();
            else if (meeting != null) {
                meeting = meeting.trim();
                for (String prefix : BiblioItem.confPrefixes) {
                    if (meeting.startsWith(prefix)) {
                        meeting = meeting.replace(prefix, "");
                        meeting = meeting.trim();
                        tei.append("\t\t\t\t\t\t<meeting>" + TextUtilities.HTMLEncode(meeting));
                        if ((biblio.getLocation() != null) || (biblio.getTown() != null) ||
                                (biblio.getCountry() != null)) {
                            tei.append(" <address>");
                            if (biblio.getTown() != null) {
                                tei.append("<settlement>" + biblio.getTown() + "</settlement>");
                            }
                            if (biblio.getCountry() != null) {
                                tei.append("<country>" + biblio.getCountry() + "</country>");
                            }
                            if ((biblio.getLocation() != null) && (biblio.getTown() == null) &&
                                    (biblio.getCountry() == null)) {
                                tei.append(TextUtilities.HTMLEncode(biblio.getLocation()));
                            }
                            tei.append("</address>\n");
                            meetLoc = true;
                        }
                        tei.append("\t\t\t\t\t\t</meeting>\n");
                        break;
                    }
                }
            }

            if (((biblio.getLocation() != null) || (biblio.getTown() != null) ||
                    (biblio.getCountry() != null))
                    && (!meetLoc)) {
                tei.append("\t\t\t\t\t\t<meeting>");
                tei.append(" <address>");
                if (biblio.getTown() != null) {
                    tei.append(" <settlement>" + biblio.getTown() + "</settlement>");
                }
                if (biblio.getCountry() != null) {
                    tei.append(" <country>" + biblio.getCountry() + "</country>");
                }
                if ((biblio.getLocation() != null) && (biblio.getTown() == null)
                        && (biblio.getCountry() == null)) {
                    tei.append(TextUtilities.HTMLEncode(biblio.getLocation()));
                }
                tei.append("</address>\n");
                tei.append("\t\t\t\t\t\t</meeting>\n");
            }

            String pageRange = biblio.getPageRange();

            if ((biblio.getVolumeBlock() != null) | (biblio.getPublicationDate() != null) |
                    (biblio.getNormalizedPublicationDate() != null) |
                    (pageRange != null) | (biblio.getIssue() != null) |
                    (biblio.getBeginPage() != -1) |
                    (biblio.getPublisher() != null)) {
                tei.append("\t\t\t\t\t\t<imprint>\n");

                if (biblio.getPublisher() != null) {
                    tei.append("\t\t\t\t\t\t\t<publisher>" + TextUtilities.HTMLEncode(biblio.getPublisher())
                            + "</publisher>\n");
                }

                if (biblio.getVolumeBlock() != null) {
                    String vol = biblio.getVolumeBlock();
                    vol = vol.replace(" ", "").trim();
                    tei.append("\t\t\t\t\t\t\t<biblScope unit=\"volume\">" +
                            TextUtilities.HTMLEncode(vol) + "</biblScope>\n");
                }

                if (biblio.getIssue() != null) {
                    tei.append("\t\t\t\t\t\t\t<biblScope unit=\"issue\">"
                            + TextUtilities.HTMLEncode(biblio.getIssue()) + "</biblScope>\n");
                }

                if (pageRange != null) {
                    StringTokenizer st = new StringTokenizer(pageRange, "--");
                    if (st.countTokens() == 2) {
                        tei.append("\t\t\t\t\t\t\t<biblScope unit=\"page\"");
						tei.append(" from=\"" + TextUtilities.HTMLEncode(st.nextToken()) + "\"");
						tei.append(" to=\"" + TextUtilities.HTMLEncode(st.nextToken()) + "\"/>\n");
                        //tei.append(">" + TextUtilities.HTMLEncode(pageRange) + "</biblScope>\n");
                    } else {
                        tei.append("\t\t\t\t\t\t\t<biblScope unit=\"page\">" + TextUtilities.HTMLEncode(pageRange)
                                + "</biblScope>\n");
                    }
                } else if (biblio.getBeginPage() != -1) {
					if (biblio.getEndPage() != -1) {
						tei.append("\t\t\t\t\t\t\t<biblScope unit=\"page\"");
						tei.append(" from=\"" + biblio.getBeginPage() + "\"");
						tei.append(" to=\"" + biblio.getEndPage() + "\"/>\n");
					}
					else {
						tei.append("\t\t\t\t\t\t\t<biblScope unit=\"page\"");
						tei.append(" from=\"" + biblio.getBeginPage() + "\"/>\n");
					}
                }

                if (biblio.getNormalizedPublicationDate() != null) {
                    Date date = biblio.getNormalizedPublicationDate();
                    int year = date.getYear();
                    int month = date.getMonth();
                    int day = date.getDay();

	                String when = "";
					if (year <= 9) 
						when += "000" + year;
					else if (year <= 99) 
						when += "00" + year;
					else if (year <= 999)
						when += "0" + year;
					else
						when += year;
	                if (month != -1) {
						if (month <= 9) 
							when += "-0" + month;
						else 
	 					   	when += "-" + month;
	                    if (day != -1) {
							if (day <= 9)
								when += "-0" + day;
							else
								when += "-" + day;
	                    }
	                }
					if (biblio.getPublicationDate() != null) {
                    	tei.append("\t\t\t\t\t\t\t<date type=\"published\" when=\"");
                    	tei.append(when + "\">");
						tei.append(TextUtilities.HTMLEncode(biblio.getPublicationDate())
                                + "</date>\n");
					}
					else {
                    	tei.append("\t\t\t\t\t\t\t<date type=\"published\" when=\"");
                    	tei.append(when + "\" />\n");
					}
                } else if (biblio.getYear() != null) {
					String when = "";
					if (biblio.getYear().length() == 1)
						when += "000" + biblio.getYear();
					else if (biblio.getYear().length() == 2)
						when += "00" + biblio.getYear();
					else if (biblio.getYear().length() == 3)
						when += "0" + biblio.getYear();
					else if (biblio.getYear().length() == 4)
						when += biblio.getYear();
			
	                if (biblio.getMonth() != null) {
						if (biblio.getMonth().length() == 1)
							when += "-0" + biblio.getMonth();
						else
							when += "-" + biblio.getMonth();
	                    if (biblio.getDay() != null) {
							if (biblio.getDay().length() == 1)
								when += "-0" + biblio.getDay();
							else
								when += "-" + biblio.getDay();
	                    }
	                }
					if (biblio.getPublicationDate() != null) {
						tei.append("\t\t\t\t\t\t\t<date type=\"published\" when=\"");
                    	tei.append(when + "\">");
						tei.append(TextUtilities.HTMLEncode(biblio.getPublicationDate())
                                + "</date>\n");
					} 
					else {
                    	tei.append("\t\t\t\t\t\t\t<date type=\"published\" when=\"");
                    	tei.append(when + "\" />\n");
					}
                } else if (biblio.getE_Year() != null) {
					String when = "";
					if (biblio.getE_Year().length() == 1)
						when += "000" + biblio.getE_Year();
					else if (biblio.getE_Year().length() == 2)
						when += "00" + biblio.getE_Year();
					else if (biblio.getE_Year().length() == 3)
						when += "0" + biblio.getE_Year();
					else if (biblio.getE_Year().length() == 4)
						when += biblio.getE_Year();
			
	                if (biblio.getE_Month() != null) {
						if (biblio.getE_Month().length() == 1)
							when += "-0" + biblio.getE_Month();
						else
							when += "-" + biblio.getE_Month();
				
	                    if (biblio.getE_Day() != null) {
							if (biblio.getE_Day().length() == 1)
								when += "-0" + biblio.getE_Day();
							else
								when += "-" + biblio.getE_Day();
	  					 }
	                }
                    tei.append("\t\t\t\t\t\t\t<date type=\"ePublished\" when=\"");
                    tei.append(when + "\" />\n");
                } else if (biblio.getPublicationDate() != null) {
                    tei.append("\t\t\t\t\t\t\t<date type=\"published\">");
                    tei.append(TextUtilities.HTMLEncode(biblio.getPublicationDate())
                            + "</date>\n");
                }
            
				// Fix for issue #31
                tei.append("\t\t\t\t\t\t</imprint>\n");
            }
			tei.append("\t\t\t\t\t</monogr>\n");
        }

        if (biblio.getDOI() != null) {
            String theDOI = biblio.getDOI();
            if (theDOI.endsWith(".xml")) {
                theDOI = theDOI.replace(".xml", "");
            }

            tei.append("\t\t\t\t\t<idno type=\"DOI\">" + theDOI + "</idno>\n");    
        }

        if (biblio.getSubmission() != null) {
            tei.append("\t\t\t\t\t<note type=\"submission\">" +
                    TextUtilities.HTMLEncode(biblio.getSubmission()) + "</note>\n");
        }

        if (biblio.getDedication() != null) {
            tei.append("\t\t\t\t\t<note type=\"dedication\">" + TextUtilities.HTMLEncode(biblio.getDedication())
                    + "</note>\n");
        }

        if ((english_title != null) & (!hasEnglishTitle)) {
            tei.append("\t\t\t\t\t<note type=\"title\">" + TextUtilities.HTMLEncode(english_title)
                    + "</note>\n");
        }

        if (biblio.getNote() != null) {
            tei.append("\t\t\t\t\t<note>" + TextUtilities.HTMLEncode(biblio.getNote()) + "</note>\n");
        }

        tei.append("\t\t\t\t</biblStruct>\n");

		if (biblio.getURL() != null) {
			tei.append("\t\t\t\t<ref target=\"" + biblio.getURL() + "\" />\n");
		}
		
        tei.append("\t\t\t</sourceDesc>\n");
        tei.append("\t\t</fileDesc>\n");

		boolean profileDescWritten = false;

        // keywords here !!
		if (biblio.getKeywords() != null) {
			if (biblio.getKeywords().size() > 0) {
				profileDescWritten = true;
				tei.append("\t\t<profileDesc>\n");
	            tei.append("\t\t\t<textClass>\n");
				tei.append("\t\t\t\t<keywords type=\"author\">");
				tei.append("\n\t\t\t\t\t<list>\n");
			
				List<String> keywords = biblio.getKeywords();
			
				int pos = 0;
				for(String keyw : keywords) {
					tei.append("\t\t\t\t\t\t<item>\n");
					String res = keyw.trim();
					if (res.startsWith(":")) {
			            res = res.substring(1);
			        }
					if (pos == (keywords.size()-1)) {
						if (res.endsWith(".")) {
				            res = res.substring(0, res.length()-1);
				        }
					}
	                tei.append("\t\t\t\t\t\t\t<term>" +
	                        TextUtilities.HTMLEncode(res) +
	                        "</term>\n");
	                tei.append("\t\t\t\t\t\t</item>\n");
					pos++;
				}
			
				tei.append("\t\t\t\t\t</list>\n");
	            tei.append("\t\t\t\t</keywords>\n");
			
				//tei.append("\t\t\t</textClass>\n");
	            //tei.append("\t\t</profileDesc>\n");
			}
		}
		else if (biblio.getKeyword() != null) {
          	String keywords = biblio.getKeyword();
			profileDescWritten = true;
			tei.append("\t\t<profileDesc>\n");
            tei.append("\t\t\t<textClass>\n");
			
			// Note: to be cleaned...
            if (keywords.startsWith("Categories and Subject Descriptors")) {
                tei.append("\t\t\t\t<keywords type=\"subject-headers\">");
            } else if (keywords.startsWith("PACS Numbers:")) {
                tei.append("\t\t\t\t<keywords type=\"pacs\">");
                keywords = keywords.replace("PACS Numbers:", "").trim();
            } else if (keywords.startsWith("PACS numbers:")) {
                tei.append("\t\t\t\t<keywords type=\"pacs\">");
                keywords = keywords.replace("PACS numbers:", "").trim();
            } else if (keywords.startsWith("PACS")) {
                tei.append("\t\t\t\t<keywords type=\"pacs\">");
                keywords = keywords.replace("PACS", "").trim();
            } else
                tei.append("\t\t\t\t<keywords type=\"author\">");
			
            int start = keywords.indexOf("Keywords");
            if (start != -1) {
                //String keywords1 = keywords.substring(0, start-1);
                keywords = keywords.substring(start + 8, keywords.length());
            }
			if (keywords.endsWith(".")) {
		          keywords = keywords.substring(0, keywords.length()-1);
			}
			
            StringTokenizer st1 = new StringTokenizer(keywords, ";");
            if (st1.countTokens() > 2) {
                tei.append("\n\t\t\t\t\t<list>\n");
                while (st1.hasMoreTokens()) {
                    tei.append("\t\t\t\t\t\t<item>\n");
					String res = st1.nextToken().trim();
					if (res.startsWith(":")) {
			            res = res.substring(1);
			        }
                    tei.append("\t\t\t\t\t\t\t<term>" +
                            TextUtilities.HTMLEncode(res) +
                            "</term>\n");
                    tei.append("\t\t\t\t\t\t</item>\n");
                }
                tei.append("\t\t\t\t\t</list>\n");
                tei.append("\t\t\t\t</keywords>\n");
            } else {
                st1 = new StringTokenizer(keywords, ",");
                if (st1.countTokens() > 2) {
                    tei.append("\n\t\t\t\t\t<list>\n");
                    while (st1.hasMoreTokens()) {
                        tei.append("\t\t\t\t\t\t<item>\n");
						String res = st1.nextToken().trim();
						if (res.startsWith(":")) {
				            res = res.substring(1);
				        }
                        tei.append("\t\t\t\t\t\t\t<term>" +
                                TextUtilities.HTMLEncode(res) +
                                "</term>\n");
                        tei.append("\t\t\t\t\t\t</item>\n");
                    }
                    tei.append("\t\t\t\t\t</list>\n");
                    tei.append("\t\t\t\t</keywords>\n");
                } else {
                    tei.append(TextUtilities.HTMLEncode(biblio.getKeyword())).append("</keywords>\n");
                }
            }
		}
		
        if (biblio.getCategories() != null) {
			if (!profileDescWritten) {
				profileDescWritten = true;
            	tei.append("\t\t<profileDesc>\n");
            	tei.append("\t\t\t<textClass>\n");
			}
			List<String> categories = biblio.getCategories();
			tei.append("\t\t\t\t<keywords type=\"category\">");
			tei.append("\n\t\t\t\t\t<list>\n");
            for (String category : categories) {
                tei.append("\t\t\t\t\t\t<item>\n");
                tei.append("\t\t\t\t\t\t\t<term>" +
                        TextUtilities.HTMLEncode(category.trim()) +
                        "</term>\n");
                tei.append("\t\t\t\t\t\t</item>\n");
            }
            tei.append("\t\t\t\t\t</list>\n");
            tei.append("\t\t\t\t</keywords>\n");
        }

		if (profileDescWritten) {
			tei.append("\t\t\t</textClass>\n");
            tei.append("\t\t</profileDesc>\n");
		}

        if ((biblio.getA_Year() != null) |
                (biblio.getS_Year() != null) |
                (biblio.getSubmissionDate() != null) |
                (biblio.getNormalizedSubmissionDate() != null)
                ) {
            tei.append("\t\t<revisionDesc>\n");
        }

        // submission and other review dates here !
        if (biblio.getA_Year() != null) {
            String when = biblio.getA_Year();
            if (biblio.getA_Month() != null) {
                when += "-" + biblio.getA_Month();
                if (biblio.getA_Day() != null) {
                    when += "-" + biblio.getA_Day();
                }
            }
            tei.append("\t\t\t\t<date type=\"accepted\" when=\"");
            tei.append(when).append("\" />\n");
        }
        if (biblio.getNormalizedSubmissionDate() != null) {
            Date date = biblio.getNormalizedSubmissionDate();
            int year = date.getYear();
            int month = date.getMonth();
            int day = date.getDay();

            String when = "" + year;
            if (month != -1) {
                when += "-" + month;
                if (day != -1) {
                    when += "-" + day;
                }
            }
            tei.append("\t\t\t\t<date type=\"submission\" when=\"");
            tei.append(when).append("\" />\n");
        } else if (biblio.getS_Year() != null) {
            String when = biblio.getS_Year();
            if (biblio.getS_Month() != null) {
                when += "-" + biblio.getS_Month();
                if (biblio.getS_Day() != null) {
                    when += "-" + biblio.getS_Day();
                }
            }
            tei.append("\t\t\t\t<date type=\"submission\" when=\"");
            tei.append(when).append("\" />\n");
        } else if (biblio.getSubmissionDate() != null) {
            tei.append("\t\t\t<date type=\"submission\">").append(TextUtilities.HTMLEncode(biblio.getSubmissionDate())).append("</date>\n");

            /*tei.append("\t\t\t<change when=\"");
			tei.append(TextUtilities.HTMLEncode(biblio.getSubmissionDate()));
			tei.append("\">Submitted</change>\n");
			*/
        }
        if ((biblio.getA_Year() != null) |
                (biblio.getS_Year() != null) |
                (biblio.getSubmissionDate() != null)
                ) {
            tei.append("\t\t</revisionDesc>\n");
        }

        tei.append("\t</teiHeader>\n");

        if (doc.getLanguage() != null) {
            tei.append("\t<text xml:lang=\"").append(doc.getLanguage()).append("\">\n");
        } else {
            tei.append("\t<text>\n");
        }
		
        if (biblio.getAbstract() != null) {
            tei.append("\t\t<front>\n");
            String abstractText = biblio.getAbstract();

			Language resLang = null;
			if (abstractText != null) {
            	LanguageUtilities languageUtilities = LanguageUtilities.getInstance();
            	resLang = languageUtilities.runLanguageId(abstractText);
			}
            if (resLang != null) {
                String resL = resLang.getLangId();
                if (!resL.equals(doc.getLanguage())) {
                    tei.append("\t\t\t<div type=\"abstract\" xml:lang=\"").append(resL).append("\">\n");
                } else {
                    tei.append("\t\t\t<div type=\"abstract\">\n");
                }
            } else if (abstractText == null) {
				tei.append("\t\t\t<div type=\"abstract\" />\n");
			} else {
                tei.append("\t\t\t<div type=\"abstract\">\n");
            }

			if (abstractText != null) {
            	String abstractHeader = biblio.getAbstractHeader();
	            if (abstractHeader == null)
	                abstractHeader = "Abstract";
	            tei.append("\t\t\t\t<head>").append(TextUtilities.HTMLEncode(abstractHeader)).append("</head>\n");
	            tei.append("<p>").append(TextUtilities.HTMLEncode(abstractText)).append("</p>\n");
        
				tei.append("\t\t\t</div>\n");
			}
			
            tei.append("\t\t</front>\n");
        }

        return tei;
    }


	/** 
	 *  Light TEI formatting of the body where only basic logical document structures are present. 
	 *  This TEI format avoids most of the risks of ill-formed TEI due to structure recognition 
	 *  errors and frequent PDF noises.
	 *  It is adapted to fully automatic process and simple exploitation of the document structures 
     *  like structured indexing and search.
	 */
	public StringBuffer toTEIBodyLight(StringBuffer buffer,
                       			 	String result,
                                	BiblioItem biblio,
                                	List<BibDataSet> bds,
                                	List<String> tokenizations,
                                	Document doc) throws Exception {
      	
		if ( (result == null) || (tokenizations == null) ) {
			buffer.append("\t\t<body/>\n");
			return buffer;
		}
		buffer.append("\t\t<body>\n");

        StringTokenizer st = new StringTokenizer(result, "\n");
        String s1 = null;
        String s2 = null;
        String lastTag = null;
		String lastOriginalTag = "";
		//System.out.println(result);
        // current token position
        int p = 0;
        boolean start = true;
        boolean openFigure = false;
        boolean headFigure = false;
        boolean descFigure = false;
        boolean tableBlock = false;

        while (st.hasMoreTokens()) {
            boolean addSpace = false;
            String tok = st.nextToken().trim();
            if (tok.length() == 0) {
                continue;
            }
            StringTokenizer stt = new StringTokenizer(tok, " \t");
            //List<String> localFeatures = new ArrayList<String>();
            int i = 0;

            boolean newLine = false;
            int ll = stt.countTokens();
            while (stt.hasMoreTokens()) {
                String s = stt.nextToken().trim();
                if (i == 0) {
					int p0 = p;
                    boolean strop = false;
                    while ((!strop) && (p < tokenizations.size())) {
                        String tokOriginal = tokenizations.get(p);
                        if (tokOriginal.equals(" ") 							 
						 || tokOriginal.equals("\u00A0")) {
                            addSpace = true;
                        } 
						else if (tokOriginal.equals("\n")) {
							newLine = true;
						}  
						else if (tokOriginal.equals(s)) {
                            strop = true;
                        }
                        p++;
                    }
					if (p >= tokenizations.size()) {
						// either we are at the end of the header, or we might have 
						// a problematic token in tokenization for some reasons
						if ((p - p0) > 1) {
							// we loose the synchronicity, so we reinit p for the next token
							p = p0;
							// and we add a space to avoid concatenated words
							addSpace = true;
						}
					}
					if (s.equals("@BULLET")) {
						s = "•";
					}
                    s2 = TextUtilities.HTMLEncode(s); // lexical token
                } else if (i == ll - 1) {
                    s1 = s; // current tag
                } else {
                    if (s.equals("LINESTART"))
                        newLine = true;
                    //localFeatures.add(s);
                }
                i++;
            }

            if (newLine && !start) {
				buffer.append("\n");
            }

            String lastTag0 = null;
            if (lastTag != null) {
                if (lastTag.startsWith("I-")) {
                    lastTag0 = lastTag.substring(2, lastTag.length());
                } else {
                    lastTag0 = lastTag;
                }
            }
            String currentTag0 = null;
            if (s1 != null) {
                if (s1.startsWith("I-")) {
                    currentTag0 = s1.substring(2, s1.length());
                } else {
                    currentTag0 = s1;
                }
            }
			// we avoid citation_marker and figure_marker tags because they introduce too much mess, 
			// they will be injected later
			String currentOriginalTag = s1;
			if (currentTag0.equals("<citation_marker>") || currentTag0.equals("<figure_marker>")) {
				currentTag0 = lastTag0;
				s1 = lastTag;
			}
			if (s1.equals("I-<paragraph>") && 
				(lastOriginalTag.endsWith("<citation_marker>") || lastOriginalTag.endsWith("<figure_marker>")) ) {
				currentTag0 = "<paragraph>";
				s1 = "<paragraph>";
			}
			lastOriginalTag = currentOriginalTag;
            boolean closeParagraph = false;
            if (lastTag != null) {
                closeParagraph = testClosingTag(buffer, currentTag0, lastTag0, s1, bds);
            }

            boolean output;

            if (!currentTag0.equals("<table>") &&
                    !currentTag0.equals("<trash>") &&
                    !currentTag0.equals("<figure_head>") &&
                    !currentTag0.equals("<label>")) {
                if (openFigure) {
                    buffer.append("\n\t\t\t</figure>\n\n");
                }
                openFigure = false;
                headFigure = false;
                descFigure = false;
                tableBlock = false;
            }
               
			output = FullTextParser.writeField(buffer, s1, lastTag0, s2, "<other>", "<note type=\"other\">", addSpace, 3);

            // for paragraph we must distinguish starting and closing tags
            if (!output) {
                if (closeParagraph) {
                    output = FullTextParser.writeFieldBeginEnd(buffer, s1, "", s2, "<paragraph>", "<p>", addSpace, 3);
				} 
				else 
				{
                    output = FullTextParser.writeFieldBeginEnd(buffer, s1, lastTag, s2, "<paragraph>", "<p>", addSpace, 3);
                }
            }
            if (!output) {
                output = FullTextParser.writeField(buffer, s1, lastTag0, s2, "<citation_marker>", "<ref type=\"biblio\">", addSpace, 3);
            }
            if (!output) {
                output = FullTextParser.writeField(buffer, s1, lastTag0, s2, "<section>", "<head>", addSpace, 3);
            }
            if (!output) {
                output = FullTextParser.writeField(buffer, s1, lastTag0, s2, "<subsection>", "<head>", addSpace, 3);
            }
            if (!output) {
                if (openFigure) {
                    output = FullTextParser.writeField(buffer, s1, lastTag0, s2, "<trash>", "<trash>", addSpace, 4);
                } else {
                    output = FullTextParser.writeField(buffer, s1, lastTag0, s2, "<trash>", 
						"<figure>\n\t\t\t\t<trash>", addSpace, 3);
                    if (output) {
                        openFigure = true;
                    }
                }
            }
            if (!output) {
                output = FullTextParser.writeField(buffer, s1, lastTag0, s2, "<equation>", "<formula>", addSpace, 3);
            }
            if (!output) {
                output = FullTextParser.writeField(buffer, s1, lastTag0, s2, "<figure_marker>", 
					"<ref type=\"figure\">", addSpace, 3);
            }
            if (!output) {
                if (openFigure) {
                    if (tableBlock && (!lastTag0.equals("<table>")) && (currentTag0.equals("<table>"))) {
                        buffer.append("\n\t\t\t</figure>\n\n");
                        output = FullTextParser.writeField(buffer, s1, lastTag0, s2, "<table>", 
							"<figure>\n\t\t\t\t<table>", addSpace, 3);
                        if (output) {
                            tableBlock = true;
                            descFigure = false;
                            headFigure = false;
                        }
                    } else {
                        output = FullTextParser.writeField(buffer, s1, lastTag0, s2, "<table>", "<table>", 
							addSpace, 4);
                        if (output) {
                            tableBlock = true;
                        }
                    }
                } else {
                    output = FullTextParser.writeField(buffer, s1, lastTag0, s2, "<table>", 
						"<figure>\n\t\t\t\t<table>", addSpace, 3);
                    if (output) {
                        openFigure = true;
                        tableBlock = true;
                    }
                }
            }
            if (!output) {
                if (openFigure) {
                    if (descFigure && (!lastTag0.equals("<label>")) && (currentTag0.equals("<label>"))) {
                        buffer.append("\n\t\t\t</figure>\n\n");
                        output = FullTextParser.writeField(buffer, s1, lastTag0, s2, "<label>",
							 "<figure>\n\t\t\t\t<figDesc>", addSpace, 3);
                        if (output) {
                            descFigure = true;
                            tableBlock = false;
                            headFigure = false;
                        }
                    } else {
                        output = FullTextParser.writeField(buffer, s1, lastTag0, s2, "<label>", 
							"<figDesc>", addSpace, 4);
                        if (output) {
                            descFigure = true;
                        }
                    }
                } else {
                    output = FullTextParser.writeField(buffer, s1, lastTag0, s2, "<label>",
						 "<figure>\n\t\t\t\t<figDesc>", addSpace, 3);
                    if (output) {
                        openFigure = true;
                        descFigure = true;
                    }
                }
            }
            if (!output) {
                if (openFigure) {
                    if (headFigure && (!lastTag0.equals("<figure_head>")) &&
                            (currentTag0.equals("<figure_head>"))) {
                        buffer.append("\n\t\t\t</figure>\n\n");
                        output = FullTextParser.writeField(buffer, s1, lastTag0, s2, "<figure_head>", 
							"<figure>\n\t\t\t\t<head>", addSpace, 3);
                        if (output) {
                            descFigure = false;
                            tableBlock = false;
                            headFigure = true;
                        }
                    } else {
                        output = FullTextParser.writeField(buffer, s1, lastTag0, s2, "<figure_head>", 
							"<head>", addSpace, 4);
                        if (output) {
                            headFigure = true;
                        }
                    }
                } else {
                    output = FullTextParser.writeField(buffer, s1, lastTag0, s2, "<figure_head>", 
						"<figure>\n\t\t\t\t<head>", addSpace, 3);
                    if (output) {
                        openFigure = true;
                        headFigure = true;
                    }
                }
            }
            // for item we must distinguish starting and closing tags
            if (!output) {
                output = FullTextParser.writeFieldBeginEnd(buffer, s1, lastTag, s2, "<item>", 
					"<item>", addSpace, 3);
            }

            lastTag = s1;
			lastTag0 = currentTag0;
			
            if (!st.hasMoreTokens()) {
                if (lastTag != null) {
                    testClosingTag(buffer, "", currentTag0, s1, bds);
                }
                if (openFigure) {
                    buffer.append("\n\t\t\t</figure>\n\n");
                }
            }
            if (start) {
                start = false;
            }
        }

        /*int i = 0; 
        boolean first = true;
        boolean listOpened = false;
        double pos = 0.0;

		// we get back the body segments
		SortedSet<DocumentPiece> documentBodyParts = doc.getDocumentPart(SegmentationLabel.BODY);
		for(DocumentPiece docPiece : documentBodyParts) {
			DocumentPointer dp1 = docPiece.a;
			DocumentPointer dp2 = docPiece.b;

			//int blockPos = dp1.getBlockPtr();
			for(int blockIndex = dp1.getBlockPtr(); blockIndex <= dp2.getBlockPtr(); blockIndex++) {
            	Block block = doc.getBlocks().get(blockIndex);
	
                String localText = block.getText().trim();
                if (localText != null) {
                    localText = localText.trim();
                    localText = TextUtilities.dehyphenize(localText);
                    localText = localText.replace("\n", " ");
                    localText = localText.replace("  ", " ");
                    localText = localText.trim();

                    if (listOpened && (!localText.startsWith("@BULLET"))) {
                        tei.append("\t\t\t\t</list>\n");
                        listOpened = false;
                    }

                    if ((doc.getBlockSectionTitles() != null) && doc.getBlockSectionTitles().contains(blockIndex)) {
                        if (!first)
                            tei.append("\t\t\t</div>\n");
                        else
                            first = false;
                        // dehyphenization of section titles	
                        localText = TextUtilities.dehyphenizeHard(localText);
						localText = localText.replace("@BULLET", "•");
                        // we try to recognize the numbering of the section titles
                        Matcher m1 = BasicStructureBuilder.headerNumbering1.matcher(localText);
                        Matcher m2 = BasicStructureBuilder.headerNumbering2.matcher(localText);
                        Matcher m3 = BasicStructureBuilder.headerNumbering3.matcher(localText);
                        Matcher m = null;
                        String numb = null;
                        if (m1.find()) {
                            numb = m1.group(0);
                            m = m1;
                        } else if (m2.find()) {
                            numb = m2.group(0);
                            m = m2;
                        } else if (m3.find()) {
                            numb = m3.group(0);
                            m = m3;
                        }
                        if (numb != null) {
                            numb = numb.replace(" ", "");
                            if (numb.endsWith("."))
                                numb = numb.substring(0, numb.length() - 1);
                            //numb = numb.replace(".","");
                            tei.append("\n\t\t\t<div n=\"" + numb + "\">\n");
                            tei.append("\t\t\t\t<head>" + localText.substring(m.end(),
                                    localText.length()).trim() + "</head>\n");
                        } else {
                            tei.append("\n\t\t\t<div>\n");
                            tei.append("\t\t\t\t<head>" + localText + "</head>\n");
                        }
                    } else if ((doc.getBlockHeadFigures() != null) && doc.getBlockHeadFigures().contains(blockIndex)) {
                        tei.append("\t\t\t<figure>\n");

                        boolean graphic = false;
                        String imag = null;
                        int innd = localText.indexOf("@IMAGE");
                        if (innd != -1) {
                            imag = localText.substring(innd + 7, localText.length());
                            localText = localText.substring(0, innd);
                            //if (imag.endsWith(".jpg")) 
                            {
                                graphic = true;
                            }
                        }

                        StringTokenizer stt = new StringTokenizer(localText, ":.");
                        if (stt.hasMoreTokens()) {
                            String prefix = stt.nextToken();
                            Matcher m = BasicStructureBuilder.figure.matcher(prefix);
                            if (m.find()) {
                                tei.append("\t\t\t\t<head>" + prefix + "</head>\n");
                                if (stt.hasMoreTokens()) {
									tei.append("\n\t\t\t\t<figDesc>");	
									// output all renaining tokens in localText, not just the next
 									while (stt.hasMoreTokens()) {
										tei.append(stt.nextToken().trim());
									}
 									tei.append("</figDesc>\n");											
                                }
                                if (graphic) {
                                    tei.append("\t\t\t\t<graphic url=\"" + imag + "\" />\n");
                                }

                            } else
                                tei.append("\t\t\t\t<head>" + localText + "</head>\n");
                        } else {
                            tei.append("\t\t\t\t<head>" + localText + "</head>\n");
                        }
                        tei.append("\t\t\t</figure>\n");
                    } else if ((doc.getBlockHeadTables() != null) && doc.getBlockHeadTables().contains(blockIndex)) {
                        tei.append("\t\t\t<table>\n");
                        tei.append("\t\t\t\t<head>" + localText + "</head>\n");
                        tei.append("\t\t\t</table>\n");
                    } else if (localText.startsWith("@BULLET")) {
                        if (!listOpened) {
                            tei.append("\t\t\t\t<list type=\"inline\">\n");
                            listOpened = true;
                        }
                        localText = localText.substring(7, localText.length()).trim();
                        int ind = localText.indexOf("@BULLET");
                        if (ind != -1) {
                            tei.append("\t\t\t\t\t<item>" + localText.substring(0, ind) + "</item>\n");
                            while (ind != -1) {
                                localText = localText.substring(ind + 7, localText.length()).trim();
								localText = TextUtilities.HTMLEncode(localText);
								localText = markReferencesTEI(localText, bds);
                                ind = localText.indexOf("@BULLET");
                                if (ind != -1) {
                                    tei.append("\t\t\t\t\t<item>" + localText.substring(0, ind) + "</item>\n");
                                } else
                                    tei.append("\t\t\t\t\t<item>" + localText + "</item>\n");
                            }
                        } else {
							localText = TextUtilities.HTMLEncode(localText);
							localText = markReferencesTEI(localText, bds);
                            tei.append("\t\t\t\t\t<item>" + localText + "</item>\n");
						}
                    } else if (localText.startsWith("@IMAGE")) {
                        String image = localText.substring(7, localText.length());
                        if (image.endsWith(".jpg"))
                            tei.append("<graphic url=\"" + image + "\" />\n");
                    } else {
                        if (localText.length() > 0) {
                            //System.out.println(i + ": " + localText);
                            double localPos = block.getX();
                            double width = block.getWidth();
                            double localPos2 = block.getY();
                            //System.out.println(localPos + " " + localPos2 + " " + width);
                            if (width > 20) {
                                localText = TextUtilities.dehyphenize(localText);
								localText = localText.replace("@BULLET", "•");
								localText = TextUtilities.HTMLEncode(localText);
								localText = markReferencesTEI(localText, bds);
                                tei.append("<p>" + localText + "</p>\n");
                            }
                        }
                    }
                }
            }

			// Fix for issue #31 - lists not being closed consistently
			if (listOpened ) {
				tei.append("\t\t\t\t</list>\n");
				listOpened = false;
			}
			
            if (!first)
                tei.append("\t\t\t</div>\n");

            tei.append("\t\t</body>\n");

            tei.append("\t\t<back>\n");

            if (doc.getAcknowledgementBlocks() != null) {
                if (doc.getAcknowledgementBlocks().size() > 0) {
                    tei.append("\t\t\t<div type=\"acknowledgements\">\n");
                    for (Integer ii : doc.getAcknowledgementBlocks()) {
                        if (!doc.getBlockReferences().contains(ii)) {
                            Block block = doc.getBlocks().get(ii);
                            String localText = block.getText();
                            if (localText != null) {
                                localText = localText.replace("@BULLET", "•").trim();
                            }
                            localText = TextUtilities.dehyphenize(localText);
                            if (localText != null) {
                                localText = localText.replace("\n", " ");
                                localText = localText.trim();
                            }
                            if (doc.getBlockSectionTitles().contains(ii)) {
                                tei.append("\t\t\t\t<head>" + TextUtilities.HTMLEncode(localText) +
                                        "</head>\n");
                            } else {
                                tei.append("\t\t\t\t<p>" + TextUtilities.HTMLEncode(localText) +
                                        "</p>\n");
                            }
                        }
                    }
                    tei.append("\t\t\t</div>\n");
                }
            }
        }*/
		
      	buffer.append("\t\t</body>\n");
		
		buffer.append("\t\t<back>\n");
		
		// we apply some overall cleaning and simplification
		String str1 = "</ref></p>\n\n\t\t\t<p>";
		String str2 = "</ref> ";
		int startPos = 0;
		while(startPos != -1) {
			startPos = buffer.indexOf(str1, startPos);
			if (startPos != -1) {
				int endPos = startPos + str1.length();
				buffer.replace(startPos, endPos, str2);
				startPos = endPos;
			}
		}
		
        return buffer;
    }
	
    /**
     * TODO some documentation
     *
     * @param buffer
     * @param currentTag0
     * @param lastTag0
     * @param currentTag
     * @return
     */
    public boolean testClosingTag(StringBuffer buffer,
                                   String currentTag0,
                                   String lastTag0,
                                   String currentTag, 
								   List<BibDataSet> bds) {
        boolean res = false;
        // reference_marker and citation_marker are two exceptions because they can be embedded

        if (!currentTag0.equals(lastTag0) || currentTag.equals("I-<paragraph>") || currentTag.equals("I-<item>")) {
            //if (currentTag0.equals("<citation_marker>") || currentTag0.equals("<figure_marker>")) {
            //    return res;
            //}
			
			// we get the enclosed text
			int ind = buffer.lastIndexOf(">");
			String text = null;
			if (ind != -1) {
				text = buffer.substring(ind+1, buffer.length()).trim();
				// cleaning
				int ind2 = buffer.lastIndexOf("<");
				buffer.delete(ind2, buffer.length());
			}
			else {
				// this should actually never happen
				text = buffer.toString().trim();
			}
			text = TextUtilities.dehyphenize(text);
			text = text.replace("\n", " ");
			text = text.replace("  ", " ");
			text = markReferencesTEI(text, bds).trim();
			if (lastTag0.equals("<section>")) {
				// let's have a look at the numbering
                // we try to recognize the numbering of the section titles
                Matcher m1 = BasicStructureBuilder.headerNumbering1.matcher(text);
                Matcher m2 = BasicStructureBuilder.headerNumbering2.matcher(text);
                Matcher m3 = BasicStructureBuilder.headerNumbering3.matcher(text);
                Matcher m = null;
                String numb = null;
                if (m1.find()) {
                    numb = m1.group(0);
                    m = m1;
                } else if (m2.find()) {
                    numb = m2.group(0);
                    m = m2;
                } else if (m3.find()) {
                    numb = m3.group(0);
                    m = m3;
                }
                if (numb != null) {
					text = text.replace(numb, "").trim();
                    numb = numb.replace(" ", "");
                    if (numb.endsWith("."))
                        numb = numb.substring(0, numb.length() - 1);
                    //numb = numb.replace(".","");
                    text = "<head n=\"" + numb + "\">" + text;
                } else {
                   	text = "<head>" + text;
                }
            }
			
            res = false;
            // we close the current tag
            if (lastTag0.equals("<other>")) {
                buffer.append("<note>" + text+ "</note>\n\n");
				
            } else if (lastTag0.equals("<paragraph>")) {
				buffer.append("<p>" + text + "</p>\n\n");
                res = true;
				// return true only when the paragraph is closed

            } else if (lastTag0.equals("<section>")) {
                buffer.append(text + "</head>\n\n");

            } else if (lastTag0.equals("<subsection>")) {
                buffer.append(text + "</head>\n\n");

            } else if (lastTag0.equals("<equation>")) {
                buffer.append("<formula>" + text + "</formula>\n\n");

            } else if (lastTag0.equals("<table>")) {
                buffer.append("<table>" + text + "</table>\n");

            } else if (lastTag0.equals("<label>")) {
                buffer.append("<figDesc>" + text + "</figDesc>\n");

            } else if (lastTag0.equals("<figure_head>")) {
                buffer.append("<head>" + text + "</head>\n\n");

            } else if (lastTag0.equals("<item>")) {
                buffer.append("<item>" + text + "</item>\n\n");

            } else if (lastTag0.equals("<trash>")) {
                buffer.append("<trash>" + text + "</trash>\n\n");

            } /*else if (lastTag0.equals("<citation_marker>")) {
                buffer.append("</ref>");
				
            } else if (lastTag0.equals("<figure_marker>")) {
                buffer.append("</ref>");

            } */
			else {
                res = false;

            }

        }
        return res;
    }

	/**
	 *  TEI formatting of the body where we try to realize the full logical document 
	 *  structure. At this stage of development, the TEI might be ill-formed due to 
	 *  the complexity of the task and PDF noises, and thus can require additional 
	 *  human editing efforts.
	 *  This formatting is adapted to more complex further scenario such as document
	 *  multi-publishing and archiving.
	 */
    public StringBuffer toTEIBodyML(StringBuffer tei,
                                    String rese,
                                    BiblioItem biblio,
                                    List<BibDataSet> bds,
                                    List<String> tokenizations,
                                    Document doc) throws Exception {
		if ( (rese == null) || (tokenizations == null) ) {
			tei.append("\t\t<body/>\n");
			return tei;
		}
		
		tei.append("\t\t<body>\n");

        elements = new ArrayList<String>();
        elements.add("body");

        int i = 0;
        boolean first = true;
        boolean listOpened = false;
        double pos = 0.0;

        StringTokenizer st = new StringTokenizer(rese, "\n");
        String s1 = null;
        String s2 = null;
        String lastTag = null;
        String currentTag0 = null;
        int p = 0; // index in the results' tokenization (st)
        int blockIndex = 0;

        BibDataSet bib = null;

        // keep track of section titles
        currentSection = new StringBuffer();

        // top node of the document structure tree
        doc.getTop().startToken = 0;
        doc.getTop().endToken = tokenizations.size();
        DocumentNode currentNode = null;

        // keep track of the starting position of a reference section - if any (i.e. not 
        // references in foot note, but complete reference section)
        int startReferencePosition = -1;

        // for keeping track of figure/table
        int startFigureHeaderPosition = -1;
        currentFigureHead = new StringBuffer();
        List<NonTextObject> ntos = new ArrayList<NonTextObject>();

        // these are the structures for keeping track of figure and table begining
        List<NonTextObject> graphicObjects = new ArrayList<NonTextObject>();
        //List<Integer> figurePositions = new ArrayList<Integer>();
        //List<Integer> tablePositions = new ArrayList<Integer>();

        // keeping track of the blocks
        int b = 0;
        Block currentBlock = null;
        List<Block> blocks = doc.getBlocks();

        // we make a first pass for getting the overall hierarchical structure of the document
        while (st.hasMoreTokens()) {
            boolean addSpace = false;
            boolean addEOL = false;
            String tok = st.nextToken().trim();
            StringTokenizer stt = new StringTokenizer(tok, " \t");

            int j = 0;
            int ll = stt.countTokens();
            while (stt.hasMoreTokens()) {
                String s = stt.nextToken().trim();
                if (j == 0) {
	                if (s.equals("@BULLET")) {
	                    s = "•";
	                }
                    s2 = TextUtilities.HTMLEncode(s); // lexical token

                    boolean strop = false;
                    while ((!strop) & (p < tokenizations.size())) {
                        String tokOriginal = tokenizations.get(p);
                        if (tokOriginal.equals(" ")
                                //| tokOriginal.equals("\n") 
                                //| tokOriginal.equals("\r") 
                                || tokOriginal.equals("\t")) {
                            if (p > 0) {
                                addSpace = true;
                            }
                            p++;
                        } else if (tokOriginal.equals("\n")) {
                            if (p > 0) {
                                addEOL = true;
                            }
                            p++;
                        } else if (tokOriginal.equals("")) {
                            p++;
                        } else if (tokOriginal.equals(s)) {
                            strop = true;
                        } else if ((p + 1 < tokenizations.size()) && (tokenizations.get(p + 1).equals(s))) {
                            p++;
                            strop = true;
                        } else if ((p + 2 < tokenizations.size()) && (tokenizations.get(p + 2).equals(s))) {
                            p = p + 2;
                            strop = true;
                        } else {
                            strop = true;
                        }
                    }
                } else if (j == ll - 1) {
                    s1 = s; // current tag
                }
                j++;
            }

            String lastTag0 = null;
            if (lastTag != null) {
                if (lastTag.startsWith("I-")) {
                    lastTag0 = lastTag.substring(2, lastTag.length());
                } else {
                    lastTag0 = lastTag;
                }
            }

            if (s1 != null) {
                if (s1.startsWith("I-")) {
                    currentTag0 = s1.substring(2, s1.length());
                } else {
                    currentTag0 = s1;
                }
            }

            while (b < blocks.size()) {
                currentBlock = blocks.get(b);
                if ((p >= currentBlock.getStartToken()) && (p <= currentBlock.getEndToken())) {
                    break;
                }
                b++;
            }

            if (currentTag0.equals("<section>")) {
                if (s1.equals("I-<section>")) {
                    if ((currentNode != null) && (currentSection.length() > 0)) {
                        currentNode.label = currentSection.toString();
                        doc.getTop().addChild(currentNode);
                        currentSection = new StringBuffer();
                        currentNode = null;
                    } else if (currentNode != null) {
                        if ((currentNode.label != null) && (currentNode.label.equals("header"))) {
                            doc.getTop().addChild(currentNode);
                        }
                    }
                    currentNode = new DocumentNode();
                    currentNode.startToken = p;
                    currentSection.append(s2);
                } else {
                    if (currentNode != null) {
                        currentNode.endToken = p;
                    } else {
                        currentNode = new DocumentNode();
                        currentNode.startToken = p;
                        currentNode.endToken = p;
                    }
                    if (addSpace) {
                        currentSection.append(" " + s2);
                    } else if (addEOL) {
                        currentSection.append("\n" + s2);
                    } else {
                        currentSection.append(s2);
                    }
                }
            } 
			else if (currentTag0.equals("<figure_head>")) {
                if (s1.equals("I-<figure_head>")) {
                    if (currentFigureHead.length() > 0) {
                        String localT = currentFigureHead.toString();
                        NonTextObject nto = new NonTextObject();
                        // figure or table?
                        Matcher m1 = BasicStructureBuilder.figure.matcher(localT);
                        Matcher m2 = BasicStructureBuilder.table.matcher(localT);
                        String numb = null;
                        if (m2.find()) {
                            // table
                            //tablePositions.add(new Integer(startFigureHeaderPosition)); 
                            nto.setType(NonTextObject.Table);
                        } else {
                            // figure or default
                            //figurePositions.add(new Integer(startFigureHeaderPosition));
                            nto.setType(NonTextObject.Figure);
                        }
                        nto.setHeader(localT);
                        nto.setStartPosition(startFigureHeaderPosition);
                        nto.setEndPosition(p - 1);
                        nto.setX1(currentBlock.getX());
                        nto.setY1(currentBlock.getY());
                        nto.setX2(currentBlock.getX() + currentBlock.getWidth());
                        nto.setY2(currentBlock.getY() + currentBlock.getHeight());
                        nto.setPage(currentBlock.getPage());
                        ntos.add(nto);
                    }
                    currentFigureHead = new StringBuffer();
                    currentFigureHead.append(s2);
                    startFigureHeaderPosition = p;
                } else {
                    if (addSpace) {
                        currentFigureHead.append(" " + s2);
                    } else {
                        currentFigureHead.append(s2);
                    }
                }
            } else {
                /*if (currentNode != null) {
                    if ((currentNode.label != null) && (currentNode.label.equals("header"))) {
                        doc.getTop().addChild(currentNode);
                        currentNode = null;
                    }
                }*/

                if (currentSection.length() > 0) {
                    currentNode.label = currentSection.toString();
                    doc.getTop().addChild(currentNode);
                    currentSection = new StringBuffer();
                    currentNode = null;
                }
            }

            if (!currentTag0.equals("<figure_head>")) {
                //if (s1.equals("I-<figure_head>")) {
                if (currentFigureHead.length() > 0) {
                    String localT = currentFigureHead.toString();
                    NonTextObject nto = new NonTextObject();
                    // figure or table?
                    Matcher m1 = BasicStructureBuilder.figure.matcher(localT);
                    Matcher m2 = BasicStructureBuilder.table.matcher(localT);
                    String numb = null;
                    if (m2.find()) {
                        // table
                        //tablePositions.add(new Integer(startFigureHeaderPosition));
                        nto.setType(NonTextObject.Table);
                    } else {
                        // figure or default
                        //figurePositions.add(new Integer(startFigureHeaderPosition));
                        nto.setType(NonTextObject.Figure);
                    }
                    nto.setHeader(localT);
                    nto.setStartPosition(startFigureHeaderPosition);
                    nto.setEndPosition(p - 1);
                    nto.setX1(currentBlock.getX());
                    nto.setY1(currentBlock.getY());
                    nto.setX2(currentBlock.getX() + currentBlock.getWidth());
                    nto.setY2(currentBlock.getY() + currentBlock.getHeight());
                    nto.setPage(currentBlock.getPage());
                    ntos.add(nto);
                }
                currentFigureHead = new StringBuffer();
                //}
            }
            p++;
        }
		// end of first pass
		
        // we build the general document structure
        DocumentNode newTop = doc.getTop().clone();
        newTop.realNumber = "0";
        if (doc.getTop().children != null) {
            ArrayList<DocumentNode> lastNodesUp = new ArrayList<DocumentNode>();
            lastNodesUp.add(newTop);
            DocumentNode lastNode = newTop;
            Matcher m = null;
            // post processing for interpreting section's header numbering
            for (DocumentNode node0 : doc.getTop().children) {
                DocumentNode node = node0.clone();
                // propagation
                DocumentNode theNode = lastNode;
                while ((theNode != null) && (theNode != newTop)) {
                    theNode.endToken = node.startToken - 1;
                    theNode = theNode.father;
                }

                // we try to recognize the numbering of the section titles
                Matcher m1 = BasicStructureBuilder.headerNumbering1.matcher(node.label);
                Matcher m2 = BasicStructureBuilder.headerNumbering2.matcher(node.label);
                Matcher m3 = BasicStructureBuilder.headerNumbering3.matcher(node.label);
                Matcher m4 = BasicStructureBuilder.headerNumbering4.matcher(node.label);

                String numb = null;
                if (m1.find()) {
                    numb = m1.group(0);
                    m = m1;
                } else if (m2.find()) {
                    numb = m2.group(0);
                    m = m2;
                } else if (m3.find()) {
                    numb = m3.group(0);
                    m = m3;
                } else if (m4.find()) {
                    numb = m4.group(0);
                    m = m4;
                }
                if (numb != null) {
                    node.label = node.label.replace(numb, "").trim();
                    StringTokenizer stok = new StringTokenizer(numb, " ,.-");
                    String blop = "";
                    while (stok.hasMoreTokens()) {
                        String tokk = stok.nextToken();
                        blop += tokk + " ";
                    }
                    node.realNumber = blop;
                    // and now let's position the current node wrt the previous ones
                    for (int k = lastNodesUp.size(); k > 0; k--) {
                        DocumentNode currentTop = lastNodesUp.get(k - 1);
                        if (blop.startsWith(currentTop.realNumber)) {
                            currentTop.addChild(node);
                            currentTop.endToken = node.endToken;
                            lastNodesUp.add(node);
                            break;
                        } else if (currentTop == newTop) {
                            newTop.addChild(node);
                            lastNodesUp.add(node);
                            break;
                        }
                    }

                } else {
                    newTop.addChild(node);
                }

                lastNode = node;
            }
            lastNode.endToken = doc.getTop().endToken;
        }
        doc.setTop(newTop);

        // if the last section ends with the begining of the reference section, we can discard the corresponding
        // node
       /* DocumentNode node0 = doc.getTop();
        while (node0 != null) {
            if (node0.children != null) {
                int lastPos = node0.children.size();
                if (lastPos > 0) {
                    node0 = node0.children.get(lastPos - 1);
                }
            } else {
                break;
            }
        }
        if (node0 != null) {
            if (node0.label != null) {
                if (node0.label.length() + 10 + node0.startToken >= startReferencePosition) {
					if (node0.father != null)
                    	node0.father.children.remove(node0);
                }
            }
        }
		*/
        
		System.out.println(doc.getTop().toString());

        // second pass for fine grained analysis of the document structure
        st = new StringTokenizer(rese, "\n");
        p = 0; // index in the results' tokenization (st)

        currentParagraph = new StringBuffer();
        currentSection = new StringBuffer();
        currentItem = new StringBuffer();
        currentCitationMarker = new StringBuffer();
        currentFigureMarker = new StringBuffer();
        //currentPage = new StringBuffer();
        //currentPageFootNote = new StringBuffer();
        currentLabel = new StringBuffer();
        currentFigureHead = new StringBuffer();
        currentEquation = new StringBuffer();

        boolean wasItalic = false;
        boolean wasBold = false;

        // graphic object identification properties based on blocks
        //ArrayList<Integer> graphics = new ArrayList<Integer>();
        //ArrayList<String> images = new ArrayList<String>();
        //ArrayList<Integer> graphicsPosition = new ArrayList<Integer>();
        int n = 0;
        for (Block bl : blocks) {
            if (bl.getText() != null) {
                int innd = bl.getText().indexOf("@IMAGE");
                if (innd != -1) {
                    //System.out.println("graphic found at " + n + " and stored at " + graphics.size());
                    NonTextObject nto = new NonTextObject();
                    //graphics.add(new Integer(n));
                    nto.setBlockNumber(n);
                    String imag = bl.getText().substring(innd + 7, bl.getText().length());
                    if (imag.indexOf(".vec") != -1) {
                        nto.setType(NonTextObject.GraphicVectoriel);
                    } else {
                        nto.setType(NonTextObject.GraphicBitmap);
                    }
                    //images.add(imag);
                    nto.addFile(imag);
                    //graphicsPosition.add(new Integer(bl.getStartToken()));
                    nto.setStartPosition(bl.getStartToken());
                    nto.setX1(bl.getX());
                    nto.setY1(bl.getY());
                    nto.setX2(bl.getX() + bl.getWidth());
                    nto.setY2(bl.getY() + bl.getHeight());
                    nto.setPage(bl.getPage());
                    graphicObjects.add(nto);
                }
            }
            //System.out.println(bl.getText());
            n++;
        }

        // we try to allocate the graphic files to the appropriate figure objects
        int nbFigures = 0;
        for (NonTextObject nto : ntos) {
            if (nto.getType() == NonTextObject.Figure) {
                nbFigures++;
            }
        }
        int nbBitmaps = 0;
        for (NonTextObject nto : graphicObjects) {
            if (nto.getType() == NonTextObject.GraphicBitmap) {
                if (nto.getFiles() != null) {
                    if (nto.getFiles().size() > 0) {
                        nbBitmaps++;
                    }
                }
            }
        }
        for (NonTextObject nto1 : graphicObjects) {
            if (nto1.getType() == NonTextObject.GraphicVectoriel) {
                continue;
            }
            double lowestDistance = 1000000.0;
            NonTextObject bestNto2 = null;
            for (NonTextObject nto2 : ntos) {
                // first, objects must be on the same page
                if (nto2.getPage() != nto1.getPage()) {
                    continue;
                }

                // figure should not contains already a non vectorial graphic -> to be reviewed!
                if (nbFigures >= nbBitmaps) {
                    if (nto2.getFiles() != null && nto2.getFiles().size() > 0) {
                        continue;
                    }
                }

                // then we compute the distance between the objects
                double distance = Math.abs(nto1.getX1() - nto2.getX1()) +
                        Math.abs(nto1.getX2() - nto2.getX2()) +
                        Math.abs(nto1.getY1() - nto2.getY1()) +
                        Math.abs(nto1.getY2() - nto2.getY2());
                if (distance < lowestDistance) {
                    lowestDistance = distance;
                    bestNto2 = nto2;
                }
            }
            if (bestNto2 != null) {
                for (String filee : nto1.getFiles()) {
                    bestNto2.addFile(filee);
                }
            }
        }

        System.out.println(doc.getTop().toString());
        System.out.println("FIGURES/TABLES");
        for (NonTextObject nto : ntos) {
            System.out.println(nto.toString());
        }
        System.out.println("GRAPHICS");
        for (NonTextObject nto : graphicObjects) {
            System.out.println(nto.toString());
        }

        // current document node in document structure
        currentNode = doc.getTop();
        int divOpened = 0;
        int labelPosition = 0; // 0: unknown, 1: before figure, 2: after figure
        //boolean hasGraphics = false;
        boolean hasFigure = false;
        boolean hasTable = false;
        boolean openFigure = false;
        boolean openTable = false;
        boolean headFigure = false;
        boolean descFigure = false;
        boolean tableBlock = false;
        b = 0;
        currentBlock = null;
        int indexFigure = 0;

        while (st.hasMoreTokens()) {
            boolean addSpace = false;
            boolean addEOL = false;
            String tok = st.nextToken().trim();

            StringTokenizer stt = new StringTokenizer(tok, " \t");
            ArrayList<String> localFeatures = new ArrayList<String>();
            int j = 0;

            boolean newLine = false;
            boolean isItalic = false;
            boolean isBold = false;
            int ll = stt.countTokens();
            while (stt.hasMoreTokens()) {
                String s = stt.nextToken().trim();
                if (j == 0) {
					s = s.replace("@BULLET", "•");
                    s2 = TextUtilities.HTMLEncode(s); // lexical token
                    //s2 = s; 

                    boolean strop = false;
                    while ((!strop) & (p < tokenizations.size())) {
                        String tokOriginal = tokenizations.get(p);
                        if (tokOriginal.equals(" ")
								|| tokOriginal.equals("\u00A0")) {
                            if (p > 0) {
                                addSpace = true;
                            }
                            p++;
                        } else if (tokOriginal.equals("\n")) {
                            if (p > 0) {
                                addEOL = true;
                            }
                            p++;
                        } else if (tokOriginal.equals("")) {
                            p++;
                        } else if (tokOriginal.equals(s)) {
                            strop = true;
                        } else if ((p + 1 < tokenizations.size()) && (tokenizations.get(p + 1).equals(s))) {
                            p++;
                            strop = true;
                        } else if ((p + 2 < tokenizations.size()) && (tokenizations.get(p + 2).equals(s))) {
                            p = p + 2;
                            strop = true;
                        } else {
                            strop = true;
                        }
                    }
                } else if (j == ll - 1) {
                    s1 = s; // current tag
                } else if (j == ITALIC_POS) {
                    // this is the italic position
                    if (s.equals("1")) {
                        isItalic = true;
                    }
                } else if (j == BOLD_POS) {
                    // this is the bold position
                    if (s.equals("1")) {
                        isBold = true;
                    }
                } else {
                    if (s.equals("LINESTART"))
                        newLine = true;
                    localFeatures.add(s);
                }
                j++;
            }

            while (b < blocks.size()) {
                currentBlock = blocks.get(b);
                if ((p >= currentBlock.getStartToken()) && (p <= currentBlock.getEndToken())) {
                    break;
                }
                b++;
            }

            //System.out.println("current block: " + b + ", hasGraphics: " + hasGraphics);
            //System.out.println(localText + "\n");

            //System.out.println(tokenizations.get(p) + " | " + s2);	
            String lastTag0 = null;
            if (lastTag != null) {
                if (lastTag.startsWith("I-")) {
                    lastTag0 = lastTag.substring(2, lastTag.length());
                } else {
                    lastTag0 = lastTag;
                }
            }

            if (s1 != null) {
                if (s1.startsWith("I-")) {
                    currentTag0 = s1.substring(2, s1.length());
                } else {
                    currentTag0 = s1;
                }
            }

            boolean res = testClosingTag(tei, currentTag0, lastTag0, s1, bds, ntos);

            if (!currentTag0.equals("<figure_head>") &&
                    !currentTag0.equals("<label>") &&
                    !currentTag0.equals("<table>") &&
                    !currentTag0.equals("<trash>")) {

                if (elements.size() > 0) {
                    String lastElement = elements.get(elements.size() - 1);
                    if (lastElement.equals("figure")) {
                        tei.append("\n\t\t\t</figure>\n\n");
                        elements.remove(elements.size() - 1);
                    }
                    /*else if ( lastElement.equals("figDesc") && (!currentTag0.equals("<figure_marker>")) 
							&& (!currentTag0.equals("<citation_marker>")) ) {
						tei.append("\n\t\t\t</figure>\n\n");
						elements.remove(elements.size()-1);
					}*/
                    else if (lastElement.equals("figDesc") && (!currentTag0.equals("<figure_marker>"))
                            && (!currentTag0.equals("<citation_marker>"))) {
                        if (elements.size() > 1) {
                            String lastElement2 = elements.get(elements.size() - 2);
                            if (lastElement.equals("figure")) {
                                tei.append("</figDesc>\n");
                                tei.append("\n\t\t\t</figure>\n\n");
                                elements.remove(elements.size() - 1);
                                elements.remove(elements.size() - 1);
                            }
                        }
                    } 
					else if (lastElement.equals("head")) {
                        if (elements.size() > 1) {
                            String lastElement2 = elements.get(elements.size() - 2);
                            if (lastElement.equals("figure")) {
                                tei.append("</head>\n");
                                tei.append("\n\t\t\t</figure>\n\n");
                                elements.remove(elements.size() - 1);
                                elements.remove(elements.size() - 1);
                            }
                        }
                    }
                }

                hasTable = false;
                hasFigure = false;
                openFigure = false;
                openTable = false;
                headFigure = false;
                descFigure = false;
                tableBlock = false;
            }

            //hasGraphics = false;
            String imag = null;
            String localText = null;
            NonTextObject currentFigure = null;

            for (NonTextObject nto : ntos) {
                if ((nto.getStartPosition() - 1 <= p) && (p <= nto.getEndPosition() + 1)) {
                    currentFigure = nto;
                    if (nto.getType() == NonTextObject.Figure) {
                        hasFigure = true;
                        hasTable = false;
                        break;
                    } else if (nto.getType() == NonTextObject.Figure) {
                        hasFigure = false;
                        hasTable = true;
                        break;
                    }
                }
            }

            if ((currentNode != null) &&
                    (currentNode.label != null) &&
                    (!currentNode.label.equals("header")) &&
                    (!currentNode.label.equals("top"))) {
                if (currentNode.endToken <= p) {
                    if ((currentNode.father == null) || (currentNode.father == doc.getTop())) {
                        if (elements.size() > 0) {
                            String lastElement = elements.get(elements.size() - 1);
                            if (lastElement.equals("div")) {
                                tei.append("\n\t\t\t</div>\n\n");
                                elements.remove(elements.size() - 1);
                                divOpened -= 1;
                            }
                        }
                    } else if ((currentNode.father.father == null) || (currentNode.father.father == doc.getTop())) {
                        if (elements.size() > 0) {
                            String lastElement = elements.get(elements.size() - 1);
                            if (lastElement.equals("div")) {
                                tei.append("\n\t\t\t\t</div>\n\n");
                                elements.remove(elements.size() - 1);
                                divOpened -= 1;

                                if ((currentNode.father.endToken <= p) && (elements.size() > 0)) {
                                    lastElement = elements.get(elements.size() - 1);
                                    if (lastElement.equals("div")) {
                                        tei.append("\n\t\t\t</div>\n\n");
                                        elements.remove(elements.size() - 1);
                                        divOpened -= 1;
                                    }
                                }
                            }
                        }
                    } else if (elements.size() > 0) {
                        String lastElement = elements.get(elements.size() - 1);
                        if (lastElement.equals("div")) {
                            tei.append("\n\t\t\t\t\t</div>\n\n");
                            elements.remove(elements.size() - 1);
                            divOpened -= 1;
                            if ((currentNode.father.father.endToken <= p) && (elements.size() > 0)) {
                                lastElement = elements.get(elements.size() - 1);
                                if (lastElement.equals("div")) {
                                    tei.append("\n\t\t\t\t</div>\n\n");
                                    elements.remove(elements.size() - 1);
                                    divOpened -= 1;
                                }
                            }
                            if ((currentNode.father.endToken <= p) && (elements.size() > 0)) {
                                lastElement = elements.get(elements.size() - 1);
                                if (lastElement.equals("div")) {
                                    tei.append("\n\t\t\t</div>\n\n");
                                    elements.remove(elements.size() - 1);
                                    divOpened -= 1;
                                }
                            }
                        }
                    }
                    currentNode = null;
                }
            }

            // current node in document structure
            currentNode = doc.getTop().getSpanningNode(p);
            //System.out.println(p + "\t" +currentNode.toString());

            if ((currentNode != null) &&
                    (currentNode.label != null) &&
                    (!currentNode.label.equals("header")) &&
                    (!currentNode.label.equals("top"))) {
                if (currentNode.startToken == p) {              
                    if ((currentNode.father == null) || (currentNode.father == doc.getTop())) {
                        tei.append("\n\t\t\t<div");
                        elements.add("div");
                        tei.append(" type=\"section\"");
						divOpened += 1;
                    } else if ((currentNode.father.father == null) || (currentNode.father.father == doc.getTop())) {
                        tei.append("\n\t\t\t\t<div");
                        elements.add("div");
                        tei.append(" type=\"subsection\"");
						divOpened += 1;
                    } else if (currentNode != doc.getTop()) {
                        tei.append("\n\t\t\t\t\t<div");
                        elements.add("div");
                        tei.append(" type=\"subsubsection\"");
						divOpened += 1;
                    }

                    if (currentNode.realNumber != null) {
                        tei.append(" n=\"" + currentNode.realNumber.trim().replace(" ", ".") + "\"");
                    }
                    tei.append(">\n");

                    if (currentNode.label != null) {
                        tei.append("\t\t\t\t<head>" + currentNode.label + "</head>\n");
                    }
                }
            }

            if (currentTag0.equals("<paragraph>")) {
                if (s1.equals("I-<paragraph>")) {
                    if (lastTag0 != null) {
                        if (lastTag0.equals("<figure_marker>") ||
                                lastTag0.equals("<citation_marker>")) {
                            /*if (isItalic) {
                                tei.append("<hi rend=\"italic\">");
                                elements.add("hi");
                                currentParagraph.append(s2);
                            } 
							else if (isBold) {
                                tei.append("<hi rend=\"bold\">");
                                elements.add("hi");
                                currentParagraph.append(s2);
                            } 
							else*/ 
							{
                                currentParagraph.append(s2);
                            }
                        } 
						/*else if (lastTag0.equals("<page>")) {
                            // it depends if we have reached or not the end of the previous
                            // paragraph
                            if (inParagraph) {
                                if (isItalic & !wasItalic) {
                                    tei.append("<hi rend=\"italic\">");
                                    elements.add("hi");
                                    currentParagraph.append(s2);
                                } else if (isBold & !wasBold) {
                                    tei.append("<hi rend=\"bold\">");
                                    elements.add("hi");
                                    currentParagraph.append(s2);
                                } else {
                                    currentParagraph.append(s2);
                                }
                                inParagraph = false;
                            } else {
                                tei.append("\n\t\t\t<p>");
                                elements.add("p");
                                if (isItalic) {
                                    tei.append("<hi rend=\"italic\">");
                                    elements.add("hi");
                                    currentParagraph.append(s2);
                                } else if (isBold) {
                                    tei.append("<hi rend=\"bold\">");
                                    elements.add("hi");
                                    currentParagraph.append(s2);
                                } else {
                                    currentParagraph.append(s2);
                                }
                            }
                        } */
						else {
                            tei.append("\n\t\t\t<p>");
                            elements.add("p");
                            /*if (isItalic) {
                                tei.append("<hi rend=\"italic\">");
                                elements.add("hi");
                                currentParagraph.append(s2);
                            } else if (isBold) {
                                tei.append("<hi rend=\"bold\">");
                                elements.add("hi");
                                currentParagraph.append(s2);
                            } else */
							{
                                currentParagraph.append(s2);
                            }
                        }
                    } 
					else {
                        tei.append("\n\t\t\t<p>");
                        elements.add("p");
                        /*if (isItalic) {
                            tei.append("<hi rend=\"italic\">");
                            elements.add("hi");
                            currentParagraph.append(s2);
                        } else if (isBold) {
                            tei.append("<hi rend=\"bold\">");
                            elements.add("hi");
                            currentParagraph.append(s2);
                        } else*/ 
						{
                            currentParagraph.append(s2);
                        }
                    }
                } else {
                    /*if (wasItalic & !isItalic) {
                        tei.append(normalizeText(currentParagraph.toString()));
                        if (elements.size() > 0) {
                            String lastElement = elements.get(elements.size() - 1);
                            if (lastElement.equals("hi")) {
                                tei.append("</hi>");
                                elements.remove(elements.size() - 1);
                            }
                        }
                        currentParagraph = new StringBuffer();
                    } 
					else if (wasBold & !isBold) {
                        tei.append(normalizeText(currentParagraph.toString()));
                        if (elements.size() > 0) {
                            String lastElement = elements.get(elements.size() - 1);
                            if (lastElement.equals("hi")) {
                                tei.append("</hi>");
                                elements.remove(elements.size() - 1);
                            }
                        }
                        currentParagraph = new StringBuffer();
                    }*/
                    if (elements.size() > 0) {
                        String lastElement = elements.get(elements.size() - 1);
                        if (!lastElement.equals("p") &&
                                !lastElement.equals("hi") &&
                                !lastElement.equals("ref") &&
                                !lastElement.equals("formula")) {
                            tei.append("\n\t\t\t<p>");
                            elements.add("p");
                        }
                    }
                    if (addSpace) {
                        /*if (isItalic & !wasItalic) {
                            tei.append(normalizeText(currentParagraph.toString()));
                            tei.append(" <hi rend=\"italic\">");
                            elements.add("hi");
                            currentParagraph = new StringBuffer();
                            currentParagraph.append(s2);
                        } else if (isBold & !wasBold) {
                            tei.append(normalizeText(currentParagraph.toString()));
                            tei.append(" <hi rend=\"bold\">");
                            elements.add("hi");
                            currentParagraph = new StringBuffer();
                            currentParagraph.append(s2);
                        } else */
						{
                            currentParagraph.append(" " + s2);
                        }
                    } 
					else if (addEOL) {
                        /*if (isItalic & !wasItalic) {
                            tei.append(normalizeText(currentParagraph.toString()));
                            tei.append("\n<hi rend=\"italic\">");
                            elements.add("hi");
                            currentParagraph = new StringBuffer();
                            currentParagraph.append(s2);
                        } else if (isBold & !wasBold) {
                            tei.append(normalizeText(currentParagraph.toString()));
                            tei.append("\n<hi rend=\"bold\">");
                            elements.add("hi");
                            currentParagraph = new StringBuffer();
                            currentParagraph.append(s2);
                        } else */
						{
                            currentParagraph.append("\n" + s2);
                        }
                    } else {
                        /*if (isItalic & !wasItalic) {
                            tei.append(normalizeText(currentParagraph.toString()));
                            tei.append("<hi rend=\"italic\">");
                            elements.add("hi");
                            currentParagraph = new StringBuffer();
                            currentParagraph.append(s2);
                        } else if (isBold & !wasBold) {
                            tei.append(normalizeText(currentParagraph.toString()));
                            tei.append("<hi rend=\"bold\">");
                            elements.add("hi");
                            currentParagraph = new StringBuffer();
                            currentParagraph.append(s2);
                        } else */
						{
                            currentParagraph.append(s2);
                        }
                    }
                }
            }
            /*else if ( currentTag0.equals("<section>") ) {
				if (s1.equals("I-<section>")) {
					if (!first) {
						tei.append("\t\t</div>\n");
					}
					else {
						first = false;
					}
					currentSection.append(s2);
				}
				else {
					if (addSpace) {
						currentSection.append(" " + s2);
					}
					else if (addEOL) {
						currentSection.append("\n" + s2);
					}
					else {
						currentSection.append(s2);
					}
				}
			}*/
            else if (currentTag0.equals("<item>")) {
                if (!listOpened) {
                    listOpened = true;
                    currentItem.append("\t\t\t<list>\n");
                    elements.add("list");
                }

                if (s1.equals("I-<item>")) {
                    if (lastTag0 != null) {
                        if (lastTag0.equals("<figure_marker>") || lastTag0.equals("<citation_marker>")) {
                            currentItem.append(s2);
                        } else {
                            currentItem.append("\t\t\t\t<item>" + s2);
                            elements.add("item");
                        }
                    } else {
                        currentItem.append("\t\t\t\t<item>" + s2);
                        elements.add("item");
                    }
                } else {
                    if (addSpace) {
                        currentItem.append(" " + s2);
                    } else if (addEOL) {
                        currentItem.append("\n" + s2);
                    } else {
                        currentItem.append(s2);
                    }
                }
            } 
			else if (currentTag0.equals("<citation_marker>")) {
                if (s1.equals("I-<citation_marker>")) {
                    currentCitationMarker.append(s2);
                } else {
                    if (addSpace | addEOL) {
                        currentCitationMarker.append(" " + s2);
                    } else {
                        currentCitationMarker.append(s2);
                    }
                }
            } 
			else if (currentTag0.equals("<figure_marker>")) {
                if (s1.equals("I-<figure_marker>")) {
                    currentFigureMarker.append(s2);
                } else {
                    if (addSpace | addEOL) {
                        currentFigureMarker.append(" " + s2);
                    } else {
                        currentFigureMarker.append(s2);
                    }
                }
            } 
			else if (currentTag0.equals("<equation>")) {
                if (s1.equals("I-<equation>")) {
                    currentEquation.append(s2);
                } else {
                    if (addSpace | addEOL) {
                        currentEquation.append(" " + s2);
                    } else {
                        currentEquation.append(s2);
                    }
                }
            } 
			else if (currentTag0.equals("<label>")) {
                if (descFigure && (!lastTag0.equals("<label>"))) {
                    if (elements.size() > 0) {
                        String lastElement = elements.get(elements.size() - 1);
                        if (lastElement.equals("table")) {
                            tei.append("\n\t\t\t</table>\n");
                            elements.remove(elements.size() - 1);
                            openTable = false;
                        } else if (lastElement.equals("figure")) {
                            tei.append("\n\t\t\t</figure>\n");
                            elements.remove(elements.size() - 1);
                            openFigure = false;
                        }
                    }

                    openFigure = false;
                    openTable = false;
                    descFigure = false;
                    tableBlock = false;
                    headFigure = false;
                }
                if (s1.equals("I-<label>")) {
                    if (elements.size() > 0) {
                        String lastElement = elements.get(elements.size() - 1);
                        if (lastElement.equals("figDesc") || lastElement.equals("figure")) {
                            openFigure = true;
                            openTable = false;
                        } else if (lastElement.equals("table")) {
                            openFigure = false;
                            openTable = true;
                        }

                        if (lastElement.equals("figDesc") &&
                                (lastTag0.equals("<figure_marker>") || lastTag0.equals("<citation_marker>"))) {
                            if (addSpace | addEOL) {
                                //currentLabel.append(" " + s2);
                                tei.append(" " + s2);
                            } else {
                                //currentLabel.append(s2);
                                tei.append(s2);
                            }
                        } else if (hasFigure && (!lastElement.equals("figure")) && (!lastElement.equals("figDesc"))) {
                            tei.append("\n\t\t\t<figure xml:id=\"f" + indexFigure + "\">\n");
                            elements.add("figure");
                            indexFigure++;
                            openFigure = true;
                            openTable = false;
                            descFigure = true;
                            tableBlock = false;
                            headFigure = false;

                            if (currentFigure != null) {
                                if (currentFigure.getFiles() != null) {
                                    for (String fil : currentFigure.getFiles()) {
                                        tei.append("\t\t\t\t<graphic url=\"" + fil.trim() + "\"/>\n");
                                    }
                                }
                            }
                            tei.append("\n\t\t\t\t<figDesc>");
                            elements.add("figDesc");
                            currentLabel.append(s2);
                        } else if ((!lastElement.equals("figure")) && (!lastElement.equals("figDesc")) && (!openTable)) {
                            if (hasFigure) {
                                tei.append("\n\t\t\t<figure xml:id=\"f" + indexFigure + "\">\n\t\t\t\t<figDesc>");
                                elements.add("figure");
                                elements.add("figDesc");
                                indexFigure++;
                                openFigure = true;
                                openTable = false;
                            } else if (hasTable) {
                                tei.append("\n\t\t\t<figure>\n\t\t\t\t<table/>\n\t\t\t\t<figDesc>");
                                elements.add("table");
                                elements.add("figDesc");
                                openFigure = false;
                                openTable = true;
                            }
                            descFigure = true;
                            tableBlock = false;
                            headFigure = false;
                            //currentLabel.append(s2);
                            tei.append(s2);
                        } else {
                            tei.append("\n\n\t\t\t\t<figDesc>" + s2);
                            elements.add("figDesc");
                        }
                    }
                } else {
                    if (addSpace | addEOL) {
                        tei.append(" " + s2);
                    } else {
                        tei.append(s2);
                    }
                }
            } else if (currentTag0.equals("<figure_head>")) {
                if (headFigure && (openFigure || openTable) && (!lastTag0.equals("<figure_head>")) &&
                        (currentTag0.equals("<figure_head>"))) {
                    if (elements.size() > 0) {
                        String lastElement = elements.get(elements.size() - 1);
                        if (lastElement.equals("table")) {
                            tei.append("\n\t\t\t</table>\n");
                            elements.remove(elements.size() - 1);
                        } else if (lastElement.equals("figure")) {
                            tei.append("\n\t\t\t</figure>\n");
                            elements.remove(elements.size() - 1);
                        }
                    }
                    openFigure = false;
                    openTable = false;
                    descFigure = false;
                    tableBlock = false;
                    headFigure = false;
                }
                if (s1.equals("I-<figure_head>")) {
                    if (elements.size() > 0) {
                        String lastElement = elements.get(elements.size() - 1);
                        if (lastElement.equals("figure") && descFigure) {
                            tei.append("\n\t\t\t</figure>\n");
                            elements.remove(elements.size() - 1);
                            openFigure = false;
                        } else if (lastElement.equals("table") && descFigure) {
                            tei.append("\n\t\t\t</table>\n");
                            elements.remove(elements.size() - 1);
                            openTable = false;
                        }
                    }

                    if (hasFigure && !openFigure && !openTable) {
                        tei.append("\n\t\t\t<figure xml:id=\"f" + indexFigure + "\">\n");
                        elements.add("figure");
                        indexFigure++;
                        openFigure = true;
                        openTable = false;
                        descFigure = false;
                        tableBlock = false;
                        headFigure = true;

                        if (currentFigure != null) {
                            if (currentFigure.getFiles() != null) {
                                for (String fil : currentFigure.getFiles()) {
                                    tei.append("\t\t\t\t<graphic url=\"" + fil.trim() + "\"/>\n");
                                }
                            }
                        }
                        tei.append("\n\t\t\t\t<head>");
                        elements.add("head");
                        if (!s2.equals(" ")) {
                            currentFigureHead.append(s2);
                        }
                    } else if (!openFigure && !openTable) {
                        tei.append("\n\t\t\t<figure>\n\t\t\t\t<head>");
                        elements.add("figure");
                        elements.add("head");
                        openFigure = false;
                        openTable = true;
                        descFigure = false;
                        tableBlock = false;
                        headFigure = true;
                        if (!s2.equals(" ")) {
                            currentFigureHead.append(s2);
                        }
                    } else {
                        // openFigure or OpenTable is already true
                        currentFigureHead.append("\n\t\t\t\t<head>");
                        elements.add("head");
                        if (!s2.equals(" ")) {
                            currentFigureHead.append(s2);
                        }
                    }
                } else {
                    if (addSpace | addEOL) {
                        currentFigureHead.append(" " + s2);
                    } else {
                        currentFigureHead.append(s2);
                    }
                }
            } 
			/*else if (currentTag0.equals("<page_footnote>")) {
                if (s1.equals("I-<page_footnote>")) {
                    if (currentPageFootNote.length() > 0) {
                        currentPageFootNote.append("</note>\n\t\t\t<note place=\"foot\">");
                    }
                    if ((!s2.matches("(_)+")) && (!s2.matches("(\\*)+"))) {
                        currentPageFootNote.append(s2);
                    }
                } else {
                    if (addSpace || addEOL) {
                        if ((currentPageFootNote.length() > 0) &&
                                (currentPageFootNote.charAt(currentPageFootNote.length() - 1) != '>')) {
                            currentPageFootNote.append(" " + s2);
                        } else {
                            if ((!s2.matches("(_)+")) && (!s2.matches("(\\*)+"))) {
                                currentPageFootNote.append(s2);
                            }
                        }
                    } else {
                        if ((currentPageFootNote.length() > 0) &&
                                (currentPageFootNote.charAt(currentPageFootNote.length() - 1) != '>')) {
                            currentPageFootNote.append(s2);
                        } else {
                            if ((!s2.matches("(_)+")) && (!s2.matches("(\\*)+"))) {
                                currentPageFootNote.append(s2);
                            }
                        }
                    }
                }
            } */
			else if (currentTag0.equals("<table>")) {
                if (openFigure) {
                    System.out.println("Warning: opened figure section contains a table element");
                } else {
                    openTable = true;
                    openFigure = false;
                }
            }
            /*else if ( currentTag0.equals("<page>") ) {
				if (s1.equals("I-<page>")) {
					currentPage.append(s2);
				}
				else {
					if (addSpace | addEOL) {
						currentPage.append(" " + s2);
					}
					else {
						currentPage.append(s2);
					}
				}	
			}*/

            if (isItalic) {
                wasItalic = true;
            } else {
                wasItalic = false;
            }

            if (isBold) {
                wasBold = true;
            } else {
                wasBold = false;
            }

            lastTag = s1;
            p++;
        }

        String lastTag0 = null;
        if (lastTag != null) {
            if (lastTag.startsWith("I-")) {
                lastTag0 = lastTag.substring(2, lastTag.length());
            } else {
                lastTag0 = lastTag;
            }
        }
        testClosingTag(tei, "", lastTag0, "", bds, ntos);

        // do we still have a div opened? 
        while (divOpened > 0) {
            for (int y = 0; y < divOpened; y++) {
                tei.append("\t");
            }
            if (elements.size() > 0) {
                String lastElement = elements.get(elements.size() - 1);
                if (lastElement.equals("div")) {
                    tei.append("\t\t</div>\n");
                    elements.remove(elements.size() - 1);
                    divOpened--;
                } else {
                    // we have a problem... there is another non closed tag which is not a div element
                    // so we close it also
                    tei.append("\t\t</" + lastElement + ">\n");
                    elements.remove(elements.size() - 1);
                    divOpened--;
                }	
            }
			else
				divOpened = 0;
        }

        if (openTable) {
            if (elements.size() > 0) {
                String lastElement = elements.get(elements.size() - 1);
                if (lastElement.equals("figure")) {
                    tei.append("\n\t\t\t</figure>\n\n");
                    elements.remove(elements.size() - 1);
                }
            }
        }

        if (openFigure) {
            if (elements.size() > 0) {
                String lastElement = elements.get(elements.size() - 1);
                if (lastElement.equals("figure")) {
                    tei.append("\n\t\t\t</figure>\n\n");
                    elements.remove(elements.size() - 1);
                }
            }
        }

		// write the footnotes
		SortedSet<DocumentPiece> documentFootnoteParts = doc.getDocumentPart(SegmentationLabel.FOOTNOTE);
		String footnotes = doc.getDocumentPartText(SegmentationLabel.FOOTNOTE);
		if (documentFootnoteParts != null) {
			for(DocumentPiece docPiece : documentFootnoteParts) {
				String footText = doc.getDocumentPieceText(docPiece);
				footText = TextUtilities.dehyphenize(footText);
				footText = footText.replace("\n", " ");
				footText = footText.replace("  ", " ").trim();
				if (footText.trim().length() < 6)
					continue;
				// pattern is <note n="1" place="foot" xml:id="no1">
				tei.append("\n\t\t\t<note place=\"foot\"");
				Matcher ma = startNum.matcher(footText);
				int currentNumber = -1;
                if (ma.find()) {
                    String groupStr = ma.group(1);
					footText = ma.group(2);
                    try {
                        currentNumber = Integer.parseInt(groupStr);
                    } catch (NumberFormatException e) {
                        currentNumber = -1;
                    }
                }
				if (currentNumber != -1) {
					tei.append(" n=\"" + currentNumber + "\"");
				}
				tei.append(">");
				tei.append(TextUtilities.HTMLEncode(footText.trim()));
				tei.append("</note>\n");
			}
		}

        boolean end = false;
        while (!end) {
            if (elements.size() == 0) {
                tei.append("\t\t</body>\n");
                end = true;
            }
            if ((elements.size() > 0) && (!end)) {
                String lastElement = elements.get(elements.size() - 1);
                if (lastElement.equals("body")) {
                    tei.append("\t\t</body>\n");
                    elements.remove(elements.size() - 1);
                    end = true;
                } else {
                    tei.append("\n\t\t\t</" + lastElement + ">\n");
                    elements.remove(elements.size() - 1);
                }
            }
        }

        tei.append("\t\t<back>\n");


        return tei;
    }

    private boolean testClosingTag(StringBuffer tei,
                                   String currentTag0,
                                   String lastTag0,
                                   String currentTag,
                                   List<BibDataSet> bds,
                                   List<NonTextObject> ntos) {
        boolean res = false;

        if (!currentTag0.equals(lastTag0)
                || currentTag.equals("I-<paragraph>")
                || currentTag.equals("I-<item>")
                || currentTag.equals("I-<section>")) {

            res = true;
            // we close the current tag

            if ((lastTag0 != null) && currentTag0.equals("<paragraph>") && lastTag0.equals("<page>")) {
                if (currentParagraph.length() > 0) {
                    String paragraphString = currentParagraph.toString().trim();
                    if (paragraphString.endsWith(".")) {
                        // we consider that we have the end of a paragraph
                        boolean end = false;
                        while ((!end) && (elements.size() > 0)) {
                            String lastElement = elements.get(elements.size() - 1);
                            if (!lastElement.equals("p")) {
                                tei.append("\n\t\t\t</" + lastElement + ">\n\n");
                                elements.remove(elements.size() - 1);
                            } else {
                                tei.append(normalizeText(paragraphString) + "</p>\n");
                                elements.remove(elements.size() - 1);
                                end = true;
                            }
                        }
                    } else {
                        // we consider that the end of page is in the middle of a paragraph
                        tei.append(normalizeText(paragraphString) + " ");
                        inParagraph = true;
                    }
                }
            } else if ((lastTag0 != null) && currentTag0.equals("<paragraph>") && lastTag0.equals("<page_footnote>")) {
                if (currentParagraph.length() > 0) {
                    String paragraphString = currentParagraph.toString().trim();
                    if (paragraphString.endsWith(".")) {
                        // we consider that we have the end of a paragraph
                        boolean end = false;
                        while ((!end) && (elements.size() > 0)) {
                            String lastElement = elements.get(elements.size() - 1);
                            if (!lastElement.equals("p")) {
                                tei.append("\n\t\t\t</" + lastElement + ">\n\n");
                                elements.remove(elements.size() - 1);
                            } else {
                                tei.append(normalizeText(paragraphString) + "</p>\n");
                                elements.remove(elements.size() - 1);
                                end = true;
                            }
                        }
                    } else {
                        // we consider that the end of page is in the middle of a paragraph
                        tei.append(normalizeText(paragraphString) + " ");
                        inParagraph = true;
                    }
                }
            } else if (currentEquation.length() > 0) {
                tei.append("\n\t<p><formula>" + normalizeText(currentEquation.toString()) + "</formula></p>\n");
                currentEquation = new StringBuffer();
            } else if (currentParagraph.length() > 0) {
                if (currentTag0.equals("<citation_marker>") ||
                        currentTag0.equals("<figure_marker>")
                    // || currentTag0.equals("<page>") 
                        ) {
                    tei.append(normalizeText(currentParagraph.toString()) + " ");
                } else if (lastTag0.equals("<citation_marker>") ||
                        lastTag0.equals("<figure_marker>")
                    // || lastTag0.equals("<page>") 
                        ) {
                    tei.append(normalizeText(currentParagraph.toString()) + " ");
                } else {
                    tei.append(normalizeText(currentParagraph.toString()));
                    boolean end = false;
                    while ((!end) && (elements.size() > 0)) {
                        String lastElement = elements.get(elements.size() - 1);
                        if (!lastElement.equals("p")) {
                            tei.append("\n\t\t\t</" + lastElement + ">\n\n");
                            elements.remove(elements.size() - 1);
                        } else {
                            tei.append("</p>\n");
                            elements.remove(elements.size() - 1);
                            end = true;
                        }
                    }
                }
                currentParagraph = new StringBuffer();
            } 
			else if (currentItem.length() > 0) {
                if (currentTag0.equals("<citation_marker>") || currentTag0.equals("<figure_marker>")) {
                    tei.append(" " + normalizeText(currentItem.toString()) + " ");
                } else if (lastTag0.equals("<citation_marker>") || lastTag0.equals("<figure_marker>")) {
                    tei.append(" " + normalizeText(currentItem.toString()) + " ");
                } else if (elements.size() > 0) {
                    tei.append(" " + normalizeText(currentItem.toString()));
                    String lastElement = elements.get(elements.size() - 1);
                    if (lastElement.equals("hi")) {
                        tei.append("\n\t\t\t</hi>\n\n");
                        elements.remove(elements.size() - 1);
                    }

                    tei.append("</item>\n");
                    elements.remove(elements.size() - 1);
                }
                currentItem = new StringBuffer();
            }
            /*else if (currentLabel.length() > 0) {
				if (currentTag0.equals("<citation_marker>") || currentTag0.equals("<figure_marker>")) {
					tei.append(normalizeText(currentLabel.toString()) + " ");
				}
				else {
					tei.append(normalizeText(currentLabel.toString()));
					if (elements.size() > 0) {
						tei.append("</figDesc>");
						elements.remove(elements.size()-1);
					}
				}
				currentLabel = new StringBuffer();
			}*/
            else if (currentFigureHead.length() > 0) {
                tei.append(normalizeText(currentFigureHead.toString()));

                if (elements.size() > 0) {
                    String lastElement = elements.get(elements.size() - 1);
                    if (lastElement.equals("head")) {
                        tei.append("</head>");
                        elements.remove(elements.size() - 1);
                    }
                }
                currentFigureHead = new StringBuffer();
            } 
			else if (currentCitationMarker.length() > 0) {
                String theRef = currentCitationMarker.toString();
                theRef = markReferencesTEI(theRef, bds);
                tei.append(theRef);
                currentCitationMarker = new StringBuffer();
            } 
			else if (currentFigureMarker.length() > 0) {
                String theRef = currentFigureMarker.toString();
                theRef = markReferencesFigureTEI(theRef, ntos);
                if ((tei.length() > 0) &&
                        ((tei.charAt(tei.length() - 1) != ' ') && (tei.charAt(tei.length() - 1) != '('))) {
                    tei.append(" " + theRef);
                } else {
                    tei.append(theRef);
                }
                currentFigureMarker = new StringBuffer();
            } 
			else if ((lastTag0 != null) && lastTag0.equals("<label>")) {
                if (!currentTag0.equals("<citation_marker>") && !currentTag0.equals("<figure_marker>")) {
                    if (elements.size() > 0) {
                        String lastElement = elements.get(elements.size() - 1);
                        if (lastElement.equals("figDesc")) {
                            tei.append("</figDesc>");
                            elements.remove(elements.size() - 1);
                        }
                    }
                }
            }
            /*else if (currentPage.length() > 0) {
				String pageChunk = currentPage.toString();
				tei.append("<page/> ");
				currentPage = new StringBuffer();
			}*/
            else {
                res = false;
            }

        }

        if ((lastTag0 != null) & (currentTag0 != null)) {
            if (lastTag0.equals("<item>") && !currentTag0.equals(lastTag0) && (elements.size() > 0)) {
                tei.append("\t\t\t</list>\n");
                elements.remove(elements.size() - 1);
                res = true;
            }
        }

        return res;
    }


    public StringBuffer toTEIReferences(StringBuffer tei, List<BibDataSet> bds) throws Exception {
 		tei.append("\t\t\t<div type=\"references\">\n\n");
        tei.append("\t\t<listBibl>\n");

        int p = 0;
		if ( (bds != null) && (bds.size() > 0)) {
      	  for (BibDataSet bib : bds) {
	            BiblioItem bit = bib.getResBib();
	            if (bit != null) {
	                tei.append("\n" + bit.toTEI(p));
	            } else {
	                tei.append("\n");
	            }
	            p++;
	        }
		}

        tei.append("\n\t\t</listBibl>\n");
        tei.append("\t\t\t</div>\n");

        return tei;
    }

    /**
     * Mark the identified references in the text body using TEI annotations. This is the old version.
     */
    /*public String markReferencesTEI2(String text, List<BibDataSet> bds) {
        if (text == null)
            return null;
        if (text.trim().length() == 0)
            return text;
        //System.out.println(text);
        text = TextUtilities.HTMLEncode(text);
        Pattern numberRef = Pattern.compile("(\\[|\\()\\d+\\w?(\\)|\\])");
        Pattern numberRefCompact =
                Pattern.compile("(\\[|\\()((\\d)+(\\w)?(\\-\\d+\\w?)?,\\s?)+(\\d+\\w?)(\\-\\d+\\w?)?(\\)|\\])");
        //Pattern numberRefVeryCompact = Pattern.compile("(\\[|\\()(\\d)+-(\\d)+(\\)|\\])");
        Pattern numberRefCompact2 = Pattern.compile("((\\[|\\()(\\d+)(-|√¢¬Ä¬ì|\u2013)(\\d+)(\\)|\\]))");

        boolean numerical = false;

        // we check if we have numerical references

        // we re-write compact references, i.e [1,2] -> [1] [2] 
        Matcher m2 = numberRefCompact.matcher(text);
        StringBuffer sb = new StringBuffer();
        boolean result = m2.find();
        // Loop through and create a new String 
        // with the replacements
        while (result) {
            String toto = m2.group(0);
            if (toto.indexOf("]") != -1) {
                toto = toto.replace(",", "] [");
                toto = toto.replace("[ ", "[");
                toto = toto.replace(" ]", "]");
            } else {
                toto = toto.replace(",", ") (");
                toto = toto.replace("( ", "(");
                toto = toto.replace(" )", ")");
            }
            m2.appendReplacement(sb, toto);
            result = m2.find();
        }
        // Add the last segment of input to 
        // the new String
        m2.appendTail(sb);
        text = sb.toString();

        // we expend the references [1-3] -> [1] [2] [3]
        Matcher m3 = numberRefCompact2.matcher(text);
        StringBuffer sb2 = new StringBuffer();
        boolean result2 = m3.find();
        // Loop through and create a new String 
        // with the replacements
        while (result2) {
            String toto = m3.group(0);
            if (toto.indexOf("]") != -1) {
                toto = toto.replace("]", "");
                toto = toto.replace("[", "");
                int ind = toto.indexOf('-');
                if (ind == -1)
                    ind = toto.indexOf('\u2013');
                if (ind != -1) {
                    try {
                        int firstIndex = Integer.parseInt(toto.substring(0, ind));
                        int secondIndex = Integer.parseInt(toto.substring(ind + 1, toto.length()));
                        toto = "";
                        boolean first = true;
                        for (int j = firstIndex; j <= secondIndex; j++) {
                            if (first) {
                                toto += "[" + j + "]";
                                first = false;
                            } else
                                toto += " [" + j + "]";
                        }
                    } catch (Exception e) {
                        throw new GrobidException("An exception occurs.", e);
                    }
                }
            } else {
                toto = toto.replace(")", "");
                toto = toto.replace("(", "");
                int ind = toto.indexOf('-');
                if (ind == -1)
                    ind = toto.indexOf('\u2013');
                if (ind != -1) {
                    try {
                        int firstIndex = Integer.parseInt(toto.substring(0, ind));
                        int secondIndex = Integer.parseInt(toto.substring(ind + 1, toto.length()));
                        toto = "";
                        boolean first = true;
                        for (int j = firstIndex; j <= secondIndex; j++) {
                            if (first) {
                                toto += "(" + j + ")";
                                first = false;
                            } else
                                toto += " (" + j + ")";
                        }
                    } catch (Exception e) {
                        throw new GrobidException("An exception occurs.", e);
                    }
                }
            }
            m3.appendReplacement(sb2, toto);
            result2 = m3.find();
        }
        // Add the last segment of input to 
        // the new String
        m3.appendTail(sb2);
        text = sb2.toString();

        String res = "";
        int p = 0;
        //text = TextUtilities.HTMLEncode(text);
		if (bds != null) {
	        for (BibDataSet bib : bds) {
	            List<String> contexts = bib.getSourceBib();
	            String marker = TextUtilities.HTMLEncode(bib.getRefSymbol());
	            BiblioItem resBib = bib.getResBib();

				if (resBib == null) {
					continue;
				}

	            // search for first author and date
	            String author = resBib.getFirstAuthorSurname();
	            if (author != null) {
	                author = author.toLowerCase();
	            }
	            String year = null;
	            Date datt = resBib.getNormalizedPublicationDate();
	            if (datt != null) {
	                if (datt.getYear() != -1) {
	                    year = "" + datt.getYear();
	                }
	            }
	            //System.out.println(author + " " + year);

	            if (marker != null) {
	                Matcher m = numberRef.matcher(marker);
	                if (m.find()) {
	                    int ind = text.indexOf(marker);
	                    if (ind != -1) {
	                        text = text.substring(0, ind) +
	                                "<ref type=\"bibr\" target=\"#b" + p + "\">" + marker
	                                + "</ref>" + text.substring(ind + marker.length(), text.length());
	                    }
	                }
	            }
	            //else 
	            {
	                if ((author != null) && (year != null) && (author.length()>1)) {
	                    int indi1 = -1;
	                    int indi2 = -1;
	                    int i = 0;
	                    boolean end = false;
	                    while (!end) {
	                        indi1 = text.toLowerCase().indexOf(author, i);
	                        indi2 = text.indexOf(year, i);
	                        int added = 1;

	                        if ((indi1 == -1) || (indi2 == -1))
	                            end = true;
	                        else if ((indi1 != -1) && (indi2 != -1) && (indi1 < indi2) &&
	                                (indi2 - indi1 > author.length())) {
	                            // we check if we don't have another instance of the author between the two indices
	                            int indi1bis = text.toLowerCase().indexOf(author, indi1 + author.length());
	                            if (indi1bis == -1) {
	                                String reference = text.substring(indi1, indi2 + 4);
	                                boolean extended = false;
	                                if (text.length() > indi2 + 5) {
	                                    if ((text.charAt(indi2 + 5) == ')') ||
	                                            (text.charAt(indi2 + 5) == ']')) {
	                                        reference += text.charAt(indi2 + 5);
	                                        extended = true;
	                                    }
	                                }
	                                if (extended) {
	                                    text = text.substring(0, indi1) +
	                                            "<ref type=\"bibr\" target=\"#b" + p + "\">" + reference + "</ref>" +
	                                            text.substring(indi2 + 5, text.length());
	                                    added = 31;
	                                } else {
	                                    text = text.substring(0, indi1) +
	                                            "<ref type=\"bibr\" target=\"#b" + p + "\">" + reference + "</ref>" +
	                                            text.substring(indi2 + 4, text.length());
	                                    added = 31;
	                                }
	                            }
	                        }
							if (!end) {
	                        	i = indi1 + author.length() + added;
	                        	if (i >= text.length()) {
	                            	end = true;
	                        	}
							}
	                    }
	                }
	            }
	            p++;
	        }
		}
        //System.out.println(text);
        return text;
    }*/
	

    /**
     * Mark using TEI annotations the identified references in the text body build with the machine learning model.
     */
    public String markReferencesTEI(String text, List<BibDataSet> bds) {
        if (text == null)
            return null;
        if (text.trim().length() == 0)
            return text;

        text = TextUtilities.HTMLEncode(text);
        boolean numerical = false;

        // we check if we have numerical references

        // we re-write compact references, i.e [1,2] -> [1] [2] 
        Matcher m2 = numberRefCompact.matcher(text);
        StringBuffer sb = new StringBuffer();
        boolean result = m2.find();
        // Loop through and create a new String 
        // with the replacements
        while (result) {
            String toto = m2.group(0);
            if (toto.indexOf("]") != -1) {
                toto = toto.replace(",", "] [");
                toto = toto.replace("[ ", "[");
                toto = toto.replace(" ]", "]");
            } else {
                toto = toto.replace(",", ") (");
                toto = toto.replace("( ", "(");
                toto = toto.replace(" )", ")");
            }
            m2.appendReplacement(sb, toto);
            result = m2.find();
        }
        // Add the last segment of input to 
        // the new String
        m2.appendTail(sb);
        text = sb.toString();

        // we expend the references [1-3] -> [1] [2] [3]
        Matcher m3 = numberRefCompact2.matcher(text);
        StringBuffer sb2 = new StringBuffer();
        boolean result2 = m3.find();
        // Loop through and create a new String 
        // with the replacements
        while (result2) {
            String toto = m3.group(0);
            if (toto.indexOf("]") != -1) {
                toto = toto.replace("]", "");
                toto = toto.replace("[", "");
                int ind = toto.indexOf('-');
                if (ind == -1)
                    ind = toto.indexOf('\u2013');
                if (ind != -1) {
                    try {
                        int firstIndex = Integer.parseInt(toto.substring(0, ind));
                        int secondIndex = Integer.parseInt(toto.substring(ind + 1, toto.length()));
                        toto = "";
                        boolean first = true;
                        for (int j = firstIndex; j <= secondIndex; j++) {
                            if (first) {
                                toto += "[" + j + "]";
                                first = false;
                            } else
                                toto += " [" + j + "]";
                        }
                    } catch (Exception e) {
                        throw new GrobidException("An exception occurs.", e);
                    }
                }
            } 
			else {
                toto = toto.replace(")", "");
                toto = toto.replace("(", "");
                int ind = toto.indexOf('-');
                if (ind == -1)
                    ind = toto.indexOf('\u2013');
                if (ind != -1) {
                    try {
                        int firstIndex = Integer.parseInt(toto.substring(0, ind));
                        int secondIndex = Integer.parseInt(toto.substring(ind + 1, toto.length()));
                        toto = "";
                        boolean first = true;
                        for (int j = firstIndex; j <= secondIndex; j++) {
                            if (first) {
                                toto += "(" + j + ")";
                                first = false;
                            } else
                                toto += " (" + j + ")";
                        }
                    } catch (Exception e) {
                        throw new GrobidException("An exception occurs.", e);
                    }
                }
            }
            m3.appendReplacement(sb2, toto);
            result2 = m3.find();
        }
        // Add the last segment of input to 
        // the new String
        m3.appendTail(sb2);
        text = sb2.toString();

        String res = "";
        int p = 0;
        //text = TextUtilities.HTMLEncode(text);
		if ( (bds != null) && (bds.size() > 0)) {
        	for (BibDataSet bib : bds) {
	            List<String> contexts = bib.getSourceBib();
	            String marker = TextUtilities.HTMLEncode(bib.getRefSymbol());
	            BiblioItem resBib = bib.getResBib();

	            if (resBib != null) {
	                // search for first author, date and possibly second author
	                String author1 = resBib.getFirstAuthorSurname();
	                String author2 = null;
	                if (author1 != null) {
	                    author1 = author1.toLowerCase();
	                }
	                String year = null;
	                Date datt = resBib.getNormalizedPublicationDate();
	                if (datt != null) {
	                    if (datt.getYear() != -1) {
	                        year = "" + datt.getYear();
	                    }
	                }
	                char extend1 = 0;
	                // we check if we have an identifier with the year (e.g. 2010b)
	                if (resBib.getPublicationDate() != null) {
	                    String dat = resBib.getPublicationDate();
	                    if ((dat != null) && (year != null)) {
	                        int ind = dat.indexOf(year);
	                        if (ind != -1) {
	                            if (ind + year.length() < dat.length()) {
	                                extend1 = dat.charAt(ind + year.length());
	                            }
	                        }
	                    }
	                }

	                List<Person> fullAuthors = resBib.getFullAuthors();
	                if (fullAuthors != null) {
	                    int nbAuthors = fullAuthors.size();
	                    if (nbAuthors == 2) {
	                        // we get the last name of the second author
	                        author2 = fullAuthors.get(1).getLastName();
	                    }
	                }
	                if (author2 != null) {
	                    author2 = author2.toLowerCase();
	                }

					// try first to match the reference marker string with marker (label) present in the 
					// bibliographical section
	                if (marker != null) {
	                    Matcher m = numberRef.matcher(marker);
	                    if (m.find()) {
	                        int ind = text.indexOf(marker);
	                        if (ind != -1) {
	                            text = text.substring(0, ind) +
	                                    "<ref type=\"bibr\" target=\"#b" + p + "\">" + marker
	                                    + "</ref>" + text.substring(ind + marker.length(), text.length());
	                        }
	                    }
	                }

					// try to match based on the author and year strings
	                if ((author1 != null) && (year != null)) {
	                    int indi1 = -1; // first author
	                    int indi2 = -1; // year
	                    int indi3 = -1; // second author if only two authors in total
	                    int i = 0;
	                    boolean end = false;

	                    while (!end) {
	                        indi1 = text.toLowerCase().indexOf(author1, i); // first author matching
	                        indi2 = text.indexOf(year, i); // year matching
	                        int added = 1;
	                        if (author2 != null) {
	                            indi3 = text.toLowerCase().indexOf(author2, i); // second author matching
	                        }
	                        char extend2 = 0;
	                        if (indi2 != -1) {
	                            if (text.length() > indi2 + year.length()) {
	                                extend2 = text.charAt(indi2 + year.length()); // (e.g. 2010b)
	                            }
	                        }
	
	                        if ((indi1 == -1) || (indi2 == -1)) {
	                            end = true;
								// no author has been found, we go on with the next biblio item
							}
	                        else if ((indi1 != -1) && (indi2 != -1) && (indi3 != -1) && (indi1 < indi2) &&
	                                (indi1 < indi3) && (indi2 - indi1 > author1.length())) {
								// this is the case with 2 authors in the marker
		
	                            if ((extend1 != 0) && (extend2 != 0) && (extend1 != extend2)) {
	                                end = true;
									// we have identifiers with the year, but they don't match
									// e.g. 2010a != 2010b
	                            } 
								else {
	                                // we check if we don't have another instance of the author between the two indices
	                                int indi1bis = text.toLowerCase().indexOf(author1, indi1 + author1.length());
	                                if (indi1bis == -1) {
	                                    String reference = text.substring(indi1, indi2 + 4);
	                                    boolean extended = false;
	                                    if (text.length() > indi2 + 4) {
	                                        if ((text.charAt(indi2 + 4) == ')') ||
	                                                (text.charAt(indi2 + 4) == ']') ||
	                                                ((extend1 != 0) && (extend2 != 0) && (extend1 == extend2))) {
	                                            reference += text.charAt(indi2 + 4);
	                                            extended = true;
	                                        }
	                                    }
										String previousText = text.substring(0, indi1);
										String followingText = "";
	                                    if (extended) {
											followingText = text.substring(indi2 + 5, text.length()); 
																// 5 digits for the year + identifier character 
	                                        text = "<ref type=\"bibr\" target=\"#b" + p + "\">" + reference + "</ref>";
	                                        added = 8;
	                                    } else {
											followingText = text.substring(indi2 + 4, text.length());
																// 4 digits for the year 
	                                        text = "<ref type=\"bibr\" target=\"#b" + p + "\">" + reference + "</ref>";
	                                        added = 7;
	                                    }
										if (previousText.length() > 2) {
											previousText = markReferencesTEI(previousText, bds);
										}
										if (followingText.length() > 2) {
											followingText = markReferencesTEI(followingText, bds);
										}
											
										return previousText+text+followingText;
	                                }
	                                end = true;
	                            }
	                        } 
							else if ((indi1 != -1) && (indi2 != -1) && (indi1 < indi2) &&
	                                (indi2 - indi1 > author1.length())) {
								// this is the case with 1 author in the marker
			
	                            if ((extend1 != 0) && (extend2 != 0) && (extend1 != extend2)) {
	                                end = true;
	                            } 
								else {
	                                // we check if we don't have another instance of the author between the two indices
	                                int indi1bis = text.toLowerCase().indexOf(author1, indi1 + author1.length());
	                                if (indi1bis == -1) {
	                                    String reference = text.substring(indi1, indi2 + 4);
	                                    boolean extended = false;
	                                    if (text.length() > indi2 + 4) {
	                                        if ((text.charAt(indi2 + 4) == ')') ||
	                                                (text.charAt(indi2 + 4) == ']') ||
	                                                ((extend1 != 0) && (extend2 != 0) & (extend1 == extend2))) {
	                                            reference += text.charAt(indi2 + 4);
	                                            extended = true;
	                                        }
	                                    }
										String previousText = text.substring(0, indi1);
										String followingText = "";
	                                    if (extended) {
											followingText = text.substring(indi2 + 5, text.length()); 
																// 5 digits for the year + identifier character
	                                        text = "<ref type=\"bibr\" target=\"#b" + p + "\">" + reference + "</ref>";
	                                        added = 8;
	                                    } 
										else {
											followingText = text.substring(indi2 + 4, text.length()); 
																// 4 digits for the year 
	                                        text = "<ref type=\"bibr\" target=\"#b" + p + "\">" + reference + "</ref>";
	                                        added = 7;
										}
	                                  	if (previousText.length() > 2) {
											previousText = markReferencesTEI(previousText, bds);
										}
										if (followingText.length() > 2) {
											followingText = markReferencesTEI(followingText, bds);
										}
											
										return previousText+text+followingText;    
	                                }
	                                end = true;
	                            }
	                        }
	                        i = indi2 + year.length() + added;
	                        if (i >= text.length()) {
	                            end = true;
	                        }
	                    }
	                }
	            }
	            p++;
	        }
		}
		
		// we have not been able to solve the bibliographical marker, but we still annotate it globally
		// without pointer - just ignoring possible punctuation at the beginning and end of the string
		//text = "<ref type=\"bibr\">" + text + "</ref>";
        return text;
    }

    public String markReferencesFigureTEI(String text, List<NonTextObject> ntos) {
        if (text == null)
            return null;
        if (text.trim().length() == 0)
            return text;

        int i = 0;
        int bestFigure = -1;
        int lowestDistance = text.length() + 1;
        for (NonTextObject nto : ntos) {
            if (nto.getType() == NonTextObject.Figure) {
                String localHeader = nto.getHeader();
                int localDistance = TextUtilities.getLevenshteinDistance(text, localHeader);
                if (localDistance < lowestDistance) {
                    bestFigure = i;
                    lowestDistance = localDistance;
                }
            }
            i++;
        }

        text = TextUtilities.HTMLEncode(text);
        if (bestFigure != -1) {
            text = "<ref type=\"figure\" target=\"#f" + bestFigure + "\">" + text + "</ref>";
        } else {
            text = "<ref type=\"figure\">" + text + "</ref>";
        }
        return text;
    }

    private String normalizeText(String localText) {
        localText = localText.trim();
        localText = TextUtilities.dehyphenize(localText);
        localText = localText.replace("\n", " ");
        localText = localText.replace("  ", " ");

        return localText.trim();
    }

}