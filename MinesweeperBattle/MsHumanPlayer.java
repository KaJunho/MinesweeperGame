import java.util.Scanner;

class MsHumanPlayer extends MsPlayer {
	Scanner stdIn = new Scanner(System.in);

	//***************************************************************
	// コンストラクタ
	public MsHumanPlayer(){ super(); }
	public MsHumanPlayer(MsBoard board){ super(board); }

	//***************************************************************
	// 行動する
	public boolean play() {
		// コマンドを入力する
		System.out.print(" Open cell / put Flag [o/f]? : ");
		char command = stdIn.next().charAt(0);
		if (command!='o' && command!='f') {
			System.out.println("- invalid command.");
			return false;
		}

		// オープンするマスの座標を入力する
		System.out.print(" x? : ");
		int x = stdIn.nextInt();
		System.out.print(" y? : ");
		int y = stdIn.nextInt();

		boolean result;
		if (command=='o') {
			// マスのオープン
			result = board.openCell(x, y);
		}
		else if (command=='f') {
			// マスを開ける
			result = board.putFlag(x, y);
		}
		else {
			System.out.println("- invalid command.");
			result = false;
		}
		return result;
	}
}