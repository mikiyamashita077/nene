from flask import Flask, render_template, request, url_for, redirect
import datetime
import os

# ファイル名をチェックする関数
from werkzeug.utils import secure_filename
# 画像のダウンロード
from flask import send_from_directory

# インスタンス生成
app = Flask(__name__, static_folder='img/')

# 画像のパスを指定してJSON形式に変換
#PEOPLE_FOLDER = os.path.join('img')
UPLOAD_FOLDER = 'img/'
ALLOWED_EXTENSIONS = set(['txt', 'pdf', 'png', 'jpg', 'jpeg', 'gif', 'avi','mp4'])

app.config['UPLOAD_FOLDER'] = UPLOAD_FOLDER#PEOPLE_FOLDER

# ファイルチェックメソッド
def allwed_file(filename):
       return '.' in filename and filename.rsplit('.', 1)[1] in ALLOWED_EXTENSIONS

              
@app.context_processor
def inject_now():
    utcTime=datetime.datetime.utcnow()
    return {'now':  utcTime.strftime('%Y-%m-%d %H:%M:%S') }

# ファイルを受け取る方法の指定
@app.route('/', methods=['GET', 'POST'])
def uploads_file():
    # リクエストがポストかどうかの判別
    if request.method == 'POST':
        # ファイルがなかった場合の処理
        if 'file' not in request.files:
            flash('ファイルがありません')
            return redirect(request.url)
        # データの取り出し postパラメータ
        file = request.files['file']
        video = request.files['video']
        # getパラメータの受け取る
    #else:
        #file = request.args.get('file')
        #video = request.args.get('video')
        # ファイルのチェック
        if file and allwed_file(file.filename) and video and allwed_file(video.filename):
            # 危険な文字を削除（サニタイズ処理）
            filename = secure_filename(file.filename)
            print(filename)
            # ファイルの保存
            file.save(os.path.join(app.config['UPLOAD_FOLDER'], filename))
            # アップロード後のページに転送
            #return redirect(url_for('output', filename=filename))
            # 危険な文字を削除（サニタイズ処理）
            video = secure_filename(video.filename)
            print(video)
            # ファイルの保存
            file.save(os.path.join(app.config['UPLOAD_FOLDER'], video))
            # アップロード後のページに転送
            return redirect(url_for('output',filename=filename,video=video))
    return '''
    <!doctype html>
    <html>
        <head>
            <meta charset="UTF-8">
            <title>
                ファイルをアップロード
            </title>
        </head>
        <body>
            <h1>
                ファイルをアップロード
            </h1>
            <form method="post" enctype="multipart/form-data">
            <p><input type="file" name="file">
            <p><input type="file" name="video">
            <input type="submit" value="Upload">
            </form>
        </body>
'''
@app.route('/<filename>/<video>', methods=['GET','POST'])#, 'POST'])
def output(filename, video):
    filename = secure_filename(filename)
    filename = os.path.join(app.config['UPLOAD_FOLDER'], filename)#filename=os.path.join(app.config['UPLOAD_FOLDER'], filename)
        
    video = secure_filename(video)
    video = os.path.join(app.config['UPLOAD_FOLDER'], video)#video=os.path.join(app.config['UPLOAD_FOLDER'], filenames)
    file_name = "sample.txt"
    with open(file_name, mode="r", encoding="cp932") as f:
            text = f.read()
    
    return render_template('output.html', filename=filename, video=video,now=inject_now(), num=text,title='出力ページ', method=request.method)


if __name__ == '__main__':
  app.run(debug=True, host='0.0.0.0',port=8888, threaded=True)
