package drivers;

public class GenerateCourseLogsDriverPD  {
	public static String[] courses = {
//			"H:\\CompPaper\\524f21\\Assignment 1",
//			"H:\\CompPaper\\524f21\\Assignment 2",
//			"H:\\CompPaper\\524f21\\Assignment 3",
//			"H:\\CompPaper\\524f21\\Assignment 4",
//			"H:\\CompPaper\\524f21\\Assignment 5",
//			"H:\\CompPaper\\524f21\\Assignment 6",
//			"H:\\CompPaper\\533s22",
//			"H:\\CompPaper\\401f16",
//			"H:\\CompPaper\\401f17",
//			"H:\\CompPaper\\401f18",
//			"D:\\sakaidownloads_anonymyzed\\Comp524\\F22"
//			"D:\\sakaidownloads_anonymyzed\\Comp524\\F21"
			"D:\\sakaidownloads_anonymyzed\\Comp533\\S21"
//			"D:\\sakaidownloads_anonymyzed\\Comp533\\S20"
//			"D:\\sakaidownloads_anonymyzed\\Comp401\\ss21"

//			"H:\\CompPaper\\524f19",
//			"H:\\CompPaper\\524f20",
//			"H:\\CompPaper\\533s18",
//			"H:\\CompPaper\\533s19",
//			"H:\\CompPaper\\533s20",
//			"H:\\CompPaper\\533s21",
//			"H:\\CompPaper\\533s22\\Assignment 2",
//			"H:\\CompPaper\\533s22\\Assignment 3",
//			"H:\\CompPaper\\533s22\\Assignment 4",
//			"H:\\CompPaper\\533s22\\Assignment 5",
//			"H:\\CompPaper\\533s22\\Assignment 6",
//			"H:\\CompPaper\\533s22\\Assignment 7",
			};
	public static void main (String[] args) {
		Driver2.setCourses(courses);
		Driver2.setAnalyze(false);
		Driver2.setHasLastAssignment(true);
		Driver2.main(args);
	}
}
