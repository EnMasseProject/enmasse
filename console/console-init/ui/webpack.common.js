const path = require('path');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const webpack = require('webpack');

module.exports = {
  entry: {
    app: './src/index.js'
  },
  plugins: [
    new HtmlWebpackPlugin({
      template: './src/index.html',
      favicon: "./src/assets/images/favicon.ico"
    }),
    new webpack.DefinePlugin(
      {
      'process.env.REACT_APP_VERSION': JSON.stringify(process.env.REACT_APP_VERSION),
      'process.env.REACT_APP_NAME': JSON.stringify(process.env.REACT_APP_NAME),
      'process.env.REACT_APP_REFRESH_RATE_MILLIS': JSON.stringify(process.env.REACT_APP_REFRESH_RATE_MILLIS)
    })
  ],
  module: {
    rules: [
      {
        test: /\.js?$/,
        use: [
          {
            loader: 'babel-loader'
          }
        ]
      },
      {
        test: /\.(svg|ttf|eot|woff|woff2)$/,
        use: {
          loader: 'file-loader',
          options: {
            name: 'fonts/[name].[ext]',
            // Limit at 50k. larger files emited into separate files
            limit: 5000
          }
        },
        include: function (input) {
          // only process modules with this loader
          // if they live under a 'fonts' or 'pficon' directory
          return (input.indexOf('fonts') > -1 || input.indexOf('pficon') > -1);
        }
      },
      {
        test: /\.(jpe?g|svg|png|gif)$/i,
        use: [{ loader: 'url-loader', options: { limit: 10000, outputPath: 'images' } }, 'img-loader']
      }
    ]
  },
  output: {
    filename: '[name].bundle.js',
    path: path.resolve(__dirname, 'dist')
  }
};
