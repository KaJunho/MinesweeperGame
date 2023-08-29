import java.util.Random;

//ゲームの盤面
public class MsBoard {
	private int width;     // 幅
	private int height;    // 高さ
	private int numBombs;           // 爆弾の個数
	private int numOpenedCells = 0; // オープンされたマスの個数
	private int numFlags       = 0; // 置かれたフラグの個数

	private int[][] currentCells; // 盤面
	private int[][] solvedCells;  // 正解の盤面(爆弾=9)

	private int lastPlayScore;    // 最後の操作のスコア

	// ターン処理中か否か
	/*
	 * 1ターン中にプレイヤーが複数回のopenCellメソッドや
	 * setFlagメソッドを呼び出さないようにするために使用
	 */
	private boolean isTurnProcessing;

	// ゲームの経過を表示するかどうかをあらわすフラグ
	private boolean isMsgShown = true;

	//***************************************************************
	// コンストラクタ
	public MsBoard(int width, int height, int numBombs) {
		initialize(width, height, numBombs);
	}

	public MsBoard(int level) {
		switch (level) {
		case 1:  initialize( 9,  9, 10); break; // 初級
		case 2:	 initialize(16, 16, 40); break; // 中級
		case 3:  initialize(30, 16, 99); break; // 上級
		default: initialize( 9,  9, 10); break;
		}
	}

	private void initialize(int width, int height, int numBombs) {
		this.width         = width;
		this.height        = height;
		this.numBombs      = numBombs;
		this.lastPlayScore = 0;

		if (numBombs>width*height-1) {
			System.out.println
			("numBombs must be less than width*height.");
			this.numBombs = width*height-1;
		}

		// 現在の盤面の初期化
		// 添え字は[行番号/height/y][列番号/width/x]の順
		currentCells = new int[height][width];
		for (int y=0; y<height; y++) {
			for (int x=0; x<width; x++) {
				currentCells[y][x] = -1; //未オープン
			}
		}
	}

	//***************************************************************
	// ターンを開始する
	public void startTurn() {
		// isTurnProcessingをtrueにしてコマンド受付状態にする
		isTurnProcessing = true;
	}

	//***************************************************************
	// 位置(x, y)のマスをオープンする
	public boolean openCell(int x, int y) {
		lastPlayScore = 0;
		if (isTurnProcessing==false) {
			System.out.println("- cannot open multiple cells a turn.");
			return false;
		}

		if (openCell(x, y, true) == false) {
			return false;
		}

		if (currentCells[y][x] == 9) {
			int sum = calcSumOpenedCells(x, y);
			if (sum==0) {
				lastPlayScore = -10;
			}
			else {
				lastPlayScore = sum * (-10);
			}
		}

		// 状態表示
		if (isMsgShown==true) {
			showCurrentBoard();
		}

		isTurnProcessing = false;
		return true;
	}

	//***************************************************************
	// 実際にマスをオープンする関数
	private boolean openCell(int x, int y, boolean isMsgShown) {
		if (x<0
			|| x>=width
			|| y<0
			|| y>=height
			|| currentCells[y][x]!=-1) {
			if (this.isMsgShown==true
					&& isMsgShown==true) {
				System.out.println("- cannot open cell (" + x + ", " + y + ")");
			}
			return false;
		}
		if (this.isMsgShown==true
				&& isMsgShown==true) {
			System.out.println("- open cell (" + x + ", " + y + ")");
		}

		// 最初のマスをオープンしたら爆弾の位置を決定
		if (numOpenedCells==0) {
			determineSolvedBoard(x, y);
		}
		numOpenedCells++;
		currentCells[y][x] = solvedCells[y][x];

		// 0をオープンした場合は周囲のマスもオープン
		if (currentCells[y][x] == 0) {
			// 周囲のセルの座標を取得
			int[][] indices = getNeighborIndices(x, y);

			for (int i=0; i<indices.length; i++) {
				int nx = indices[i][0];
				int ny = indices[i][1];
				if (currentCells[ny][nx]==-1) {
					openCell(nx, ny, false);
				}
			}
		}

		return true;
	}

	//***************************************************************
	// 最初に位置 (x, y) のマスをオープンした後，正解の盤面を決める
	private void determineSolvedBoard(int x, int y) {
		// 正解の盤面を保持する配列の実体を作成
		solvedCells = new int[height][width];

		//-----------------------------------------------------------
		/* Fisher-Yatesのアルゴリズムを用いて爆弾の位置を決定
		 * 配列orderをシャッフルする(numBombs+1個の乱数を取り出せる)
		 *
		 * i を 0 から numBombs まで増加させながら以下を実行する
		 *  - j に i 以上 order.length 未満のランダムな整数を代入する
		 *  - order[j] と order[i] を交換する
		 */
		Random rand = new Random();
		int[] order = new int[height*width];
		for (int i=0; i<height*width; i++) {
			order[i] = i;
		}
		for (int i=0; i<=numBombs; i++) {
			int j = i + rand.nextInt(order.length-i);
			int tmp  = order[j];
			order[j] = order[i];
			order[i] = tmp;
		}

		//-----------------------------------------------------------
		// 爆弾の配置
		int nb=0; // 置かれた爆弾の数
		for (int i=0; ;i++) {
			int r = order[i];
			int bx = r % width;
			int by = r / width;
			// オープンされたマスには爆弾を置かない
			if (bx==x && by==y) {
				continue;
			}
			solvedCells[by][bx] = 9;
			nb++;
			if (nb==numBombs) {
				break;
			}
		}

		//-----------------------------------------------------------
		// 正解の盤面の計算
		for (int cy=0; cy<height; cy++) {
			for (int cx=0; cx<width; cx++) {
				if (solvedCells[cy][cx]==9) {
					continue;
				}
				// for文が深くなるので別メソッドに
				solvedCells[cy][cx] = countBombs(cx, cy);
			}
		}
	}

	//***************************************************************
	// 位置 (x, y) のマスにフラグを立てる
	public boolean putFlag(int x, int y) {
		lastPlayScore = 0;
		if (x<0
			|| x>=width
			|| y<0
			|| y>=height
			|| currentCells[y][x]!=-1) {
			if (this.isMsgShown==true
				&& isMsgShown==true) {
				System.out.println("- cannot put flag (" + x + ", " + y + ")");
			}
			return false;
		}

		// 結果の表示と得点の計算
		if (solvedCells[y][x]!=9) {
			if (this.isMsgShown==true) {
				System.out.println("- put flag (" + x + ", " + y + ") ... miss");
			}
			int sum = calcSumOpenedCells(x,y);
			if (sum==0) {
				lastPlayScore = -100;
			}
			else {
				lastPlayScore = sum * getFlagMissRatio(x, y);
			}
		}
		else {
			if (this.isMsgShown==true) {
				System.out.println("- put flag (" + x + ", " + y + ") ... success");
			}
			currentCells[y][x] = 10;
			numFlags++;
			int sum = calcSumOpenedCells(x,y);
			if (sum==0) {
				lastPlayScore = 50;
			}
			else {
				lastPlayScore = sum * getFlagSuccessRatio(x, y);
			}
		}

		// 状態表示
		if (isMsgShown==true) {
			showCurrentBoard();
		}

		isTurnProcessing = false;
		return true;
	}

	//***************************************************************
	// 位置 (x, y) のマスの状態を返す
	public int getCell(int x, int y) {
		return currentCells[y][x];
	}

	//***************************************************************
	// 位置(x, y)のマスの周囲の爆弾の数を返す
	private int countBombs(int x, int y) {
		// 周囲のセルの座標を取得
		int[][] indices = getNeighborIndices(x, y);

		int numLocalBombs = 0;
		for (int i=0; i<indices.length; i++) {
			int nx = indices[i][0];
			int ny = indices[i][1];
			if (solvedCells[ny][nx]==9) {
				numLocalBombs++;
			}
		}
		return numLocalBombs;
	}

	//***************************************************************
	// 位置(x, y)のマスの周囲のオープンな数字の合計を求める
	private int calcSumOpenedCells(int x, int y) {
		// 周囲のセルの座標を取得
		int[][] indices = getNeighborIndices(x, y);

		int sum = 0;
		for (int i=0; i<indices.length; i++) {
			int nx = indices[i][0];
			int ny = indices[i][1];
			if (currentCells[ny][nx]!=-1
					&& currentCells[ny][nx]<9) {
					sum += currentCells[ny][nx];
			}
		}
		return sum;
	}

	// 位置(x, y)のフラグ立てに成功した際の倍率を求める
	private int getFlagSuccessRatio(int x, int y) {
		int max   = seekMaxCell(x, y);
		int ratio = 0;
		if (max<=4)      { ratio =   1; }
		else if (max==5) { ratio =  10; }
		else if (max==6) { ratio =  20; }
		else if (max==7) { ratio =  50; }
		else if (max==8) { ratio = 100; }
		return ratio;
	}

	// 位置(x, y)のフラグ立てに失敗した際の倍率を求める
	private int getFlagMissRatio(int x, int y) {
		int max   = seekMaxCell(x, y);
		int ratio = 0;
		if (max<=4)      { ratio = -10; }
		else if (max==5) { ratio = -20; }
		else if (max==6) { ratio = -30; }
		else if (max==7) { ratio = -40; }
		return ratio;
	}

	// 位置(x, y)の周囲でオープンになった数字の最大値を求める
	private int seekMaxCell(int x, int y) {
		int max   = 0;
		int[][] indices = getNeighborIndices(x, y);

		for (int i=0; i<indices.length; i++) {
			int nx = indices[i][0];
			int ny = indices[i][1];
			if (currentCells[ny][nx]<9
					&& max<currentCells[ny][nx]) {
					max = currentCells[ny][nx];
			}
		}

		return max;
	}

	//***************************************************************
	// ゲームが終了したかどうかを判定する
	public boolean isGameOver() {
		if (numOpenedCells + numFlags == height * width)
		{
			// 正解の盤面を表示する
			// showSolvedBoard();
			return true;
		}
		return false;
	}

	//***************************************************************
	// 周辺のマスの座標の集合を返す
	/*
	 * マスの位置によって周辺のマスの数が異なるので作成
	 * 後ろの添え字は0: x座標，1: y座標をあらわすものとする
	 */
	private int[][] getNeighborIndices(int x, int y)
	{
		int indices[][];
		// 周辺のマスの数が3の場合
		if ((x==0 && y==0)
				|| (x==0 && y==height-1)
				|| (x==width-1 && y==0)
				|| (x==width-1 && y==height-1)) {
			indices = new int[3][2];
		}
		// 周辺のマスの数が5の場合
		else if (x==0
					|| x==width-1
					|| y==0
					|| y==height-1) {
			indices = new int[5][2];
		}
		// 周辺のマスの数が8の場合
		else {
			indices = new int[8][2];
		}

		int numNeighborCells = 0;
		for (int dy=-1; dy<=1; dy++) {
			if ((y==0 && dy==-1)                 // 最上行の上
					|| (y==height-1 && dy==1)) { // 最下行の下
				continue;
			}
			for (int dx=-1; dx<=1; dx++) {
				if ((x==0 && dx==-1)             // 最左列の左
					|| (x==width-1 && dx==1)) {  // 最右列の右
					continue;
				}
				if (dy==0 && dx==0) {            // 自分自身
					continue;
				}
				indices[numNeighborCells][0] = x+dx;
				indices[numNeighborCells][1] = y+dy;
				numNeighborCells++;
			}
		}
		return indices;
	}

	//***************************************************************
	// アクセッサ
	public int getWidth()          { return width; }
	public int getHeight()         { return height; }
	public int getNumBombs()       { return numBombs; }
	public int getNumOpenedCells() { return numOpenedCells; }
	public int getLastPlayScore()  { return lastPlayScore; }

	public void setMsgShown(boolean flag) { isMsgShown = flag; }
	public boolean isMsgShown()           { return isMsgShown; }


	//***************************************************************
	// 現在の盤面を表示する
	private void showCurrentBoard() { showCells(currentCells); }

	// 正解の盤面を表示する
	@SuppressWarnings("unused")
	private void showSolvedBoard()  { showCells(solvedCells);  }

	// 盤面を表示するメソッドの本体
	private void showCells(int[][] cells) {
		for (int y=0; y<height; y++) {
			for (int x=0; x<width; x++) {
				switch (cells[y][x]) {
				case -1:
					System.out.print(".");
					break;
				case 9:
					System.out.print("*");
					break;
				case 10:
					System.out.print("F");
					break;
				default:
					System.out.print(cells[y][x]);
					break;
				}
				System.out.print(" ");
			}
			System.out.println();
		}
		System.out.println();
	}
}
