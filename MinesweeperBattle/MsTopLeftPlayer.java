class MsTopLeftPlayer extends MsPlayer {
	//***************************************************************
	// コンストラクタ
	public MsTopLeftPlayer(){ super(); }
	public MsTopLeftPlayer(MsBoard board){ super(board); }

	//***************************************************************
	// 行動する
	public boolean play() {
		int x, y;

		// 左上から順にオープンされていないマスを開く
		for (int i=0; /* 無限ループ条件 */ ; i++) {
			x = i % board.getWidth();
			y = i / board.getHeight();
			if (board.getCell(x, y) == -1) {
				break;
			}
		}

		return board.openCell(x, y);
	}
}
