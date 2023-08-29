//プレイヤーのスーパークラス（抽象クラス）
public abstract class MsPlayer {
	// 持ち時間[ms]
	private static final long allottedTime = 120000; // 2 min
	// プレイヤーの数
	private static int numPlayers = 0;

	protected int     id;            // ID
	protected String  name;          // 名前 (クラス名から自動取得)
	protected int     score;         // スコア
	protected long    remainingTime; // 残り時間[ms]
	protected boolean isPlayable;    // 反則負けになっていないか
	protected MsBoard board;

	//***************************************************************
	// コンストラクタ
	public MsPlayer(){
		id    = numPlayers;
		numPlayers++;
		name  = id + "/" + this.getClass().getSimpleName();
		score = 0;
		remainingTime = allottedTime;
		isPlayable = true;
	}

	public MsPlayer(MsBoard board){
		this();
		this.board = board;
	}

	//***************************************************************
	// 行動する (抽象メソッド:サブクラスで必ず実装する)
	abstract public boolean play();

	// スコアを加算する
	public void addScore(int score) {
		this.score += score;
	}

	// 残り時間を減らす
	public void subtractTime(long time)
	{
		this.remainingTime -= time;
	}

	// プレイヤーの状態をリセットする
	public void reset(MsBoard board) {
		this.board = board;
		score      = 0;
		remainingTime = allottedTime;
		isPlayable = true;
	}

	//***************************************************************
	// アクセッサ
	public String getName() { return name;  }
	public int getScore() { return score; }
	public long getRemainingTime() { return remainingTime; }
	public boolean isPlayable() { return isPlayable; }
	public void unsetPlayable() { isPlayable = false; }
	public String getStatus() {
		return name
				+ (isPlayable?
						(" : score " + score
								+ " (" + (remainingTime/1000.0) + "[s] remaining)"):
						(" : LOST"));
	}
}
