
module Redcar
  class MenuItem
    attr_reader :text, :command
    
    def initialize(text, command)
      @text, @command = text, command
    end
  end
end