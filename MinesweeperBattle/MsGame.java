import java.util.ArrayList;
import java.util.Scanner;

// ゲームの進行を管理するクラス
public class MsGame {
	Scanner stdIn = new Scanner(System.in);

	// 盤面
	private MsBoard  board;
	// プレイヤーのリスト (可変長配列ArrayListで保持)
	private ArrayList<MsPlayer> players = new ArrayList<MsPlayer>();

	// ゲーム中であるかどうかをあらわすフラグ
	private boolean isStarted = false;
	// ゲームの経過を表示するかどうかをあらわすフラグ
	private boolean isMsgShown = true;
	// デバッグ用のフラグ
	boolean isDebugMode = false;

	//***************************************************************
	// コンストラクタ
	public MsGame() {}

	public MsGame(MsBoard board) {
		this();
		this.board = board;
	}

	//***************************************************************
	// ゲームにプレイヤーを登録する
	public void addPlayer(MsPlayer player) {
		if (isStarted == false) {
			players.add(player);
		}
		else {
			System.out.println
			("ERROR: players cannot be added after game started.");
		}
	}

	//***************************************************************
	// ゲームを進行する
	public MsPlayer start() {
		// プレイヤーが1人以上登録されているかチェックする
		if (players.size()<1) {
			System.out.println("ERROR: no players added.");
			return null;
		}

		isStarted = true;
		int turn = 0;
		while (true) {
			// ターンの開始処理
			board.startTurn();

			// そのターンでプレイするプレイヤーの決定と表示
			MsPlayer player = players.get(turn%players.size());
			if (isMsgShown) {
				System.out.println();
				System.out.println
				("Turn " + turn + ": " + player.getName());
			}
			if (!player.isPlayable())
			{
				System.out.println("- skipped.");
				turn++;
				continue;
			}

			// プレイヤーの行動
			int loopCounter = 0;
			long startTime = System.currentTimeMillis();
			while (true) {
				boolean canOpen = player.play();
				if (canOpen==false) {
					System.out.println("- play again.");
					player.addScore(-100);
					loopCounter++;
				}
				else {
					break;
				}

				// 間違えすぎたら反則負け
				if (loopCounter>=5) {
					System.out.println("OOPS! TOO MANY FAULT!");
					System.out.println(player.getName() + " lost...");
					player.unsetPlayable();
					break;
				}
			}
			long endTime = System.currentTimeMillis();
			player.addScore(board.getLastPlayScore());
			player.subtractTime(endTime - startTime);

			// プレイヤー名，得点，残り時間の表示
			if (isMsgShown) {
				for (int i=0; i<players.size(); i++) {
					System.out.println
					("- " + players.get(i).getStatus());
				}
			}

			// 残り時間が無くなったら反則負け
			if (player.getRemainingTime()<0)
			{
				System.out.println("OOPS! NO REMAINING TIME!");
				System.out.println(player.getName() + " lost...");
				player.unsetPlayable();
}

			// 終了判定
			boolean isGameOver = true;
			for (int i=0; i<players.size(); i++)
			{
				if (players.get(i).isPlayable())
				{
					isGameOver = false;
					break;
				}
			}
			if (!isGameOver)
			{
				isGameOver = board.isGameOver();
			}
			if (isGameOver==true) {
				System.out.println("\nFinished.");
				break;
			}

			if (isDebugMode==true) {
				System.out.print("Press Enter....");
				stdIn.nextLine();
			}

			turn++;
		}
		for (int i=0; i<players.size(); i++) {
			System.out.println
			("- " + players.get(i).getStatus());
		}

		isStarted = false;
		return getWinner();
	}

	//***************************************************************
	// 勝者を返す
	public MsPlayer getWinner() {
		MsPlayer winner = null;
		for (int i=0; i<players.size(); i++) {
			// 反則負けしたプレイヤーは除外
			if (!players.get(i).isPlayable())
			{
				continue;
			}
			else if (winner == null)
			{
				winner = players.get(i);
				continue;
			}
			// 得点の高い者が勝ち
			if (players.get(i).getScore()>winner.getScore()) {
				winner = players.get(i);
			}
			// 同点の場合は残り時間の多い者が勝ち
			else if (players.get(i).getScore()==winner.getScore()
					&& players.get(i).getRemainingTime()
					> winner.getRemainingTime()){
				winner = players.get(i);
			}
			// それでも同点の場合はIDの若い者が勝ち
		}
		return winner;
	}

	//***************************************************************
	// アクセッサ
	public void setMsgShown(boolean flag) {
		isMsgShown = flag;
		board.setMsgShown(flag);
	}

	public boolean isMsgShown()            { return isMsgShown;  }
	public void setDebugMode(boolean flag) { isDebugMode = flag; }
	public boolean isDebugMode()           { return isDebugMode; }
}