// 対戦型マインスイーパ
public class MsBattle {
	// メインメソッド
	public static void main(String[] args) {
		// ゲームのレベル
		int level = 1; // 1: 初級, 2: 中級, 3: 上級
		// 盤面の生成
		MsBoard board = new MsBoard(level);

		// 引き続き下のような盤面の生成も可能
		// MsBoard board = new MsBoard(9, 9, 50);

		// プレイヤーの生成
		MsPlayer player1 = new MsTopLeftPlayer(board);
		MsPlayer player2 = new MsJ1200036(board);

		//-------------------------------------------------------
		// ゲームの生成
		MsGame game = new MsGame(board);

		// 各ターンの出力を抑制する場合は下を呼び出す．
		//game.setMsgShown(false);

		// 各ターンで止めEnterキー入力を促す場合は下を呼び出す．
		game.setDebugMode(true);

		// ゲームへのプレイヤーの登録
		game.addPlayer(player1);
		game.addPlayer(player2);

		//-------------------------------------------------------
		// ゲームのスタート (勝者を返す)
		MsPlayer winner = game.start();

		if (winner!=null) {
			System.out.println("\nWinner : " + winner.getName());
		}
	}
}
