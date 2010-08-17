
module Redcar
  class Runnables
    class CommandOutputController
      include Redcar::HtmlController
      
      attr_accessor :cmd

      def initialize(path, cmd, title)
        @path = path
        @cmd = cmd
        @title = title
        @output_id = 0
      end
      
      def title
        @title
      end
      
      def ask_before_closing
        if @shell
          "This tab contains an unfinished process. \n\nKill the process and close?"
        end
      end
      
      def close
        if @shell
          Process.kill(9, @shell.pid.to_i + 1)
        end
      end
      
      def run
        execute <<-JAVASCRIPT
          $('.output').hide();
        JAVASCRIPT

        case Redcar.platform
        when :osx, :linux
          run_posix
        when :windows
          run_windows
        end
      end
      
      def stylesheet_link_tag(*files)
        files.map do |file|
          path = File.join(Redcar.root, %w(plugins runnables views) + [file.to_s + ".css"])
          url = "file://" + File.expand_path(path)
          %Q|<link href="#{url}" rel="stylesheet" type="text/css" />|
        end.join("\n")
      end
      
      def process(text)
        @processor ||= OutputProcessor.new
        @processor.process(text)
      end
      
      def run_windows
        @thread = Thread.new do
          output = `cd #{@path} & #{@cmd} 2>&1`
          start_new_output_block
          append_output <<-HTML
            <div class="stdout">
              #{process(output)}
            </div>
          HTML
          end_output_block
        end
      end
      
      def format_time(time)
        time.strftime("%I:%M:%S %p").downcase
      end

      def start_output_block
        @start = Time.now
        @output_id += 1
        append_to_container <<-HTML
          <div class="process running">
            <div id="header#{@output_id}" class="header" onclick="$(this).next().slideToggle();">
              <span class="in-progress-message">Processing since #{format_time(@start)}</span>
            </div>
            <div id="output#{@output_id}" class="output"></div>
          </div>
        HTML
      end

      def end_output_block
        @end = Time.now
        append_to("#header#{@output_id}", <<-HTML)
          <span class="completed-message">Completed at #{format_time(@end)}. (Took #{@end - @start} seconds)</span>
        HTML
        execute <<-JS
          $("#output#{@output_id}").parent().removeClass("running");
        JS
      end

      def append_to(container, html)
        execute(<<-JAVASCRIPT)
          $(#{html.inspect}).appendTo("#{container}");
          $("html, body").attr({ scrollTop: $("#{container}").attr("scrollHeight") });
        JAVASCRIPT
      end

      def append_to_container(html)
        append_to("#container", html)
      end

      def append_output(output)
        append_to("#output#{@output_id}", output)
      end

      def run_posix
        @thread = Thread.new do
          sleep 1
          @shell = Session::Shell.new
          @shell.outproc = lambda do |out|
            append_output <<-HTML
              <div class="stdout">
                #{process(out)}
              </div>
            HTML
          end
          @shell.errproc = lambda do |err|
            append_output <<-HTML
              <div class="stderr">
                #{process(err)}
              </div>
            HTML
          end
          start_output_block
          begin
            @shell.execute("cd #{@path}; " + @cmd)
          rescue => e
            puts e.class
            puts e.message
            puts e.backtrace
          end
          end_output_block
          @shell = nil
          @thread = nil
        end
      end        
      
      def index
        rhtml = ERB.new(File.read(File.join(File.dirname(__FILE__), "..", "..", "views", "command_output.html.erb")))
        command = @cmd
        run
        rhtml.result(binding)
      end
    end
  end
end

